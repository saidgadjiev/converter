package ru.gadjini.telegram.converter.service.conversion.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.common.FFmpegHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.EDIT;

@Component
public class VideoEditor extends BaseAny2AnyConverter {

    private static final String TAG = "vedit";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(EDIT)
    );

    private ConversionMessageBuilder messageBuilder;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    private FFmpegHelper fFmpegHelper;

    private Gson gson;

    private LocalisationService localisationService;

    @Autowired
    public VideoEditor(ConversionMessageBuilder messageBuilder, UserService userService, FFprobeDevice fFprobeDevice,
                       FFmpegDevice fFmpegDevice, FFmpegHelper fFmpegHelper, Gson gson, LocalisationService localisationService) {
        super(MAP);
        this.messageBuilder = messageBuilder;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.fFmpegHelper = fFmpegHelper;
        this.gson = gson;
        this.localisationService = localisationService;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            SettingsState settingsState = gson.fromJson((JsonElement) fileQueueItem.getExtra(), SettingsState.class);
            Integer height = Integer.valueOf(settingsState.getResolution().replace("p", ""));
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), 0);

            if (Objects.equals(srcWhd.getHeight(), height)) {
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_RESOLUTION_THE_SAME,
                        new Object[]{settingsState.getResolution()}, userService.getLocaleOrDefault(fileQueueItem.getUserId())));
            }
            if (srcWhd.getHeight() != null && height > srcWhd.getHeight()) {
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_RESOLUTION_CANT_BE_INCREASED,
                        new Object[]{srcWhd.getHeight() + "p", settingsState.getResolution()}, userService.getLocaleOrDefault(fileQueueItem.getUserId())));
            }

            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegHelper.removeExtraVideoStreams(allStreams);

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());

            String scale = "scale=-2:" + height;
            for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); videoStreamIndex++) {
                commandBuilder.mapVideo(videoStreamIndex);
                fFmpegHelper.addFastestVideoCodecOptions(commandBuilder, file, result, videoStreams.get(videoStreamIndex), videoStreamIndex, scale);
                commandBuilder.filterVideo(videoStreamIndex, scale);
            }
            if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
                commandBuilder.mapAudio().copyAudio();
            }
            if (allStreams.stream().anyMatch(s -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))) {
                if (fFmpegHelper.isSubtitlesCopyable(file, result)) {
                    commandBuilder.mapSubtitles().copySubtitles();
                } else if (FFmpegHelper.isSubtitlesSupported(fileQueueItem.getFirstFileFormat())) {
                    commandBuilder.mapSubtitles();
                    FFmpegHelper.addSubtitlesCodec(commandBuilder, fileQueueItem.getFirstFileFormat());
                }
            }
            commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
            commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);
            fFmpegHelper.addTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());

            fFmpegDevice.convert(file.getAbsolutePath(), result.getAbsolutePath(), commandBuilder.build());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            FFprobeDevice.WHD targetWhd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            String resolutionChangedInfo = messageBuilder.getResolutionChangedInfoMessage(srcWhd.getHeight(),
                    height, userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), targetWhd.getWidth(), height,
                        targetWhd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), resolutionChangedInfo);
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem), resolutionChangedInfo);
            }
        } catch (UserException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}

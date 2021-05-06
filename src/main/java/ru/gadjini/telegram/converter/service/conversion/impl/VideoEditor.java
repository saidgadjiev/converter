package ru.gadjini.telegram.converter.service.conversion.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamsChangeHelper;
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

    private Gson gson;

    private LocalisationService localisationService;

    private FFmpegVideoStreamsChangeHelper videoStreamsChangeHelper;

    private FFmpegSubtitlesHelper fFmpegHelper;

    @Autowired
    public VideoEditor(ConversionMessageBuilder messageBuilder, UserService userService, FFprobeDevice fFprobeDevice,
                       FFmpegDevice fFmpegDevice, Gson gson, LocalisationService localisationService,
                       FFmpegVideoStreamsChangeHelper videoStreamsChangeHelper, FFmpegSubtitlesHelper fFmpegHelper) {
        super(MAP);
        this.messageBuilder = messageBuilder;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.gson = gson;
        this.localisationService = localisationService;
        this.videoStreamsChangeHelper = videoStreamsChangeHelper;
        this.fFmpegHelper = fFmpegHelper;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        fileQueueItem.setTargetFormat(fileQueueItem.getFirstFileFormat());
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            fFmpegHelper.validateVideoIntegrity(file);
            SettingsState settingsState = gson.fromJson((JsonElement) fileQueueItem.getExtra(), SettingsState.class);
            Integer height = Integer.valueOf(settingsState.getResolution().replace("p", ""));
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), 0);

            if (Objects.equals(srcWhd.getHeight(), height)) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_RESOLUTION_THE_SAME,
                        new Object[]{settingsState.getResolution()}, userService.getLocaleOrDefault(fileQueueItem.getUserId())));
            }
            String scale = "scale=-2:" + height;
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
            commandBuilder.input(file.getAbsolutePath());
            videoStreamsChangeHelper.prepareCommandForVideoScaling(commandBuilder, file, result, scale, fileQueueItem.getTargetFormat());
            if (srcWhd.getHeight() != null && height > srcWhd.getHeight()) {
                //Так как при увличении разрешения и так снижается качество
                commandBuilder.crf("30");
             }
            commandBuilder.out(result.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

            FFprobeDevice.WHD targetWhd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            String resolutionChangedInfo = messageBuilder.getResolutionChangedInfoMessage(srcWhd.getHeight(),
                    height, userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            if (fileQueueItem.getTargetFormat().canBeSentAsVideo()) {
                return new VideoResult(fileName, result, fileQueueItem.getTargetFormat(), downloadThumb(fileQueueItem), targetWhd.getWidth(), height,
                        targetWhd.getDuration(), fileQueueItem.getTargetFormat().supportsStreaming(), resolutionChangedInfo);
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem), resolutionChangedInfo);
            }
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}

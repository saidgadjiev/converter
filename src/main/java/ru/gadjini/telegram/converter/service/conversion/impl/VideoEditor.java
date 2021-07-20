package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoCrfState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoCommandPreparer;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.EDIT;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

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

    private Jackson jackson;

    private FFmpegVideoCommandPreparer videoStreamsChangeHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper;

    private CaptionGenerator captionGenerator;

    @Autowired
    public VideoEditor(ConversionMessageBuilder messageBuilder, UserService userService, FFprobeDevice fFprobeDevice,
                       FFmpegDevice fFmpegDevice, Jackson jackson,
                       FFmpegVideoCommandPreparer videoStreamsChangeHelper,
                       FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper,
                       FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                       FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper, CaptionGenerator captionGenerator) {
        super(MAP);
        this.messageBuilder = messageBuilder;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.jackson = jackson;
        this.videoStreamsChangeHelper = videoStreamsChangeHelper;
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.subtitlesStreamConversionHelper = subtitlesStreamConversionHelper;
        this.captionGenerator = captionGenerator;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            videoStreamConversionHelper.validateVideoIntegrity(file);
            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);

            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(),
                    FFmpegVideoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));

            String height = settingsState.getResolution().replace("p", "");
            String scale = EditVideoResolutionState.DONT_CHANGE.equals(settingsState.getResolution()) ? null
                    : "scale=-2:" + (NumberUtils.isDigits(height) ? height : "ceil(ih" + height + "/2)*2");

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
            commandBuilder.hideBanner().quite().input(file.getAbsolutePath());

            if (StringUtils.isBlank(scale)) {
                if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                    videoStreamConversionHelper.convertVideoCodecsForTelegramVideo(commandBuilder,
                            allStreams, fileQueueItem.getFirstFileFormat(), fileQueueItem.getSize());
                } else {
                    videoStreamConversionHelper.convertVideoCodecs(commandBuilder, allStreams,
                            fileQueueItem.getFirstFileFormat(), result, fileQueueItem.getSize());
                }
                videoStreamConversionHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());

                FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
                if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                    audioStreamInVideoFileConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
                } else {
                    audioStreamInVideoFileConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, allStreams,
                            result, fileQueueItem.getFirstFileFormat());
                }
                subtitlesStreamConversionHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder,
                        allStreams, result, fileQueueItem.getFirstFileFormat());
                commandBuilder.fastConversion();
            } else {
                videoStreamsChangeHelper.prepareCommandForVideoScaling(commandBuilder, allStreams, result, scale,
                        fileQueueItem.getFirstFileFormat(), false, fileQueueItem.getSize());
            }

            if (fileQueueItem.getFirstFileFormat() == WEBM) {
                commandBuilder.vp8QualityOptions();
            }
            if (!EditVideoCrfState.DONT_CHANGE.equals(settingsState.getCrf())) {
                commandBuilder.crf(settingsState.getCrf());
            }

            commandBuilder.defaultOptions().out(result.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            FFprobeDevice.WHD targetWhd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            String resolutionChangedInfo = messageBuilder.getVideoEditedInfoMessage(fileQueueItem.getSize(),
                    result.length(), srcWhd.getHeight(),
                    targetWhd.getHeight(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));

            String caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource(), resolutionChangedInfo);
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem),
                        targetWhd.getWidth(), targetWhd.getHeight(),
                        targetWhd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), caption);
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem), caption);
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

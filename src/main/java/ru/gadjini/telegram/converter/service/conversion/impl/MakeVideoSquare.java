package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegCommandPreparer;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.SQUARE;

@Component
@SuppressWarnings("CPD-START")
public class MakeVideoSquare extends BaseAny2AnyConverter {

    private static final String TAG = "vsquare";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(Format.filter(FormatCategory.VIDEO), List.of(SQUARE));
    }};

    public static final Format TARGET_FORMAT = Format.MP4;

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private FFmpegCommandPreparer videoStreamsChangeHelper;

    private FFmpegVideoHelper fFmpegVideoHelper;

    public MakeVideoSquare(FFmpegDevice fFmpegDevice, LocalisationService localisationService,
                           UserService userService, FFprobeDevice fFprobeDevice,
                           FFmpegCommandPreparer videoStreamsChangeHelper,
                           FFmpegVideoHelper fFmpegVideoHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.videoStreamsChangeHelper = videoStreamsChangeHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, TARGET_FORMAT.getExt());
        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), FFmpegVideoConversionHelper.getFirstVideoStreamIndex(allStreams));

            if (Objects.equals(srcWhd.getHeight(), srcWhd.getWidth())) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_ALREADY_SQUARE,
                        new Object[]{srcWhd.getWidth() + "x" + srcWhd.getHeight()}, userService.getLocaleOrDefault(fileQueueItem.getUserId())))
                        .setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
            commandBuilder.hideBanner().quite().input(file.getAbsolutePath());
            int size = Math.max(srcWhd.getHeight(), srcWhd.getWidth());
            String scale = "scale='iw:ih':force_original_aspect_ratio=decrease,pad=" + size + ":" + size + ":(ow-iw)/2:(oh-ih)/2";
            videoStreamsChangeHelper.prepareCommandForVideoScaling(commandBuilder, allStreams, result, scale, TARGET_FORMAT);
            commandBuilder.defaultOptions().out(result.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), TARGET_FORMAT.getExt());

            String caption = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SQUARE_CAPTION,
                    new Object[]{size + "x" + size}, userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            if (TARGET_FORMAT.canBeSentAsVideo()) {
                return new VideoResult(fileName, result, TARGET_FORMAT, downloadThumb(fileQueueItem),
                        size, size,
                        srcWhd.getDuration(), TARGET_FORMAT.supportsStreaming(), caption);
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

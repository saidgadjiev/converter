package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler;
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
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.COMPRESS;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.filter;

@Component
public class VideoCompressConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoCompressConverter.class);

    private static final String TAG = "vcompress";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(filter(FormatCategory.VIDEO), List.of(COMPRESS));
    }};

    private static final String SCALE = "scale=-2:ceil(ih/3)*2";

    public static final int DEFAULT_QUALITY = 70;

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private ConversionMessageBuilder messageBuilder;

    private FFmpegVideoCommandPreparer videoStreamsChangeHelper;

    private CaptionGenerator captionGenerator;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    @Autowired
    public VideoCompressConverter(FFmpegDevice fFmpegDevice, LocalisationService localisationService, UserService userService,
                                  FFprobeDevice fFprobeDevice, ConversionMessageBuilder messageBuilder,
                                  FFmpegVideoCommandPreparer videoStreamsChangeHelper,
                                  CaptionGenerator captionGenerator, FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                                  FFmpegProgressCallbackHandlerFactory callbackHandlerFactory) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.messageBuilder = messageBuilder;
        this.videoStreamsChangeHelper = videoStreamsChangeHelper;
        this.captionGenerator = captionGenerator;
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.callbackHandlerFactory = callbackHandlerFactory;
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
            commandBuilder.hideBanner().quite().input(file.getAbsolutePath());

            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(file.getAbsolutePath(),
                    videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            videoStreamsChangeHelper.prepareCommandForVideoScaling(commandBuilder, allStreams, result,
                    fileQueueItem.getFirstFileFormat());

            commandBuilder.defaultOptions().out(result.getAbsolutePath());

            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(fileQueueItem, whd.getDuration(), locale);
            fFmpegDevice.execute(commandBuilder.buildFullCommand(), callback);

            LOGGER.debug("Compress({}, {}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(),
                    fileQueueItem.getFirstFileFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), MemoryUtils.humanReadableByteCount(result.length()));

            if (fileQueueItem.getSize() <= result.length()) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCOMPRESSIBLE_VIDEO, locale))
                        .setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            String compessionInfo = messageBuilder.getVideoCompressionInfoMessage(fileQueueItem.getSize(), result.length(),
                    userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            String caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource(), compessionInfo);
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), caption);
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

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }
}

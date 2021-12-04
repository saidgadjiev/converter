package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler;
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
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

    public static final int DEFAULT_QUALITY = 70;

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private ConversionMessageBuilder messageBuilder;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private FFmpegConversionContextPreparerChain conversionContextPreparer;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public VideoCompressConverter(FFmpegDevice fFmpegDevice, LocalisationService localisationService, UserService userService,
                                  FFprobeDevice fFprobeDevice, ConversionMessageBuilder messageBuilder,
                                  FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                                  FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                                  FFmpegConversionContextPreparerChainFactory contextPreparerFactory,
                                  FFmpegCommandBuilderFactory commandBuilderFactory, VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.messageBuilder = messageBuilder;
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.conversionContextPreparer = contextPreparerFactory.videoConversionContextPreparer();
        this.videoResultBuilder = videoResultBuilder;

        conversionContextPreparer.setNext(contextPreparerFactory.videoCompression());

        this.commandBuilderChain = commandBuilderFactory.quiteInput();
        commandBuilderChain.setNext(commandBuilderFactory.simpleVideoStreamsConversion())
                .setNext(commandBuilderFactory.videoCompression())
                .setNext(commandBuilderFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderFactory.output());
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
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getStreams(file.getAbsolutePath(), FormatCategory.VIDEO);
            FFmpegConversionContext conversionContext = FFmpegConversionContext.from(file, result, fileQueueItem.getFirstFileFormat(), allStreams);
            conversionContextPreparer.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);

            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(file.getAbsolutePath(),
                    videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(fileQueueItem, whd.getDuration(), locale);

            fFmpegDevice.execute(command.toCmd(), callback);

            LOGGER.debug("Compress({}, {}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(),
                    fileQueueItem.getFirstFileFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), MemoryUtils.humanReadableByteCount(result.length()));

            if (fileQueueItem.getSize() <= result.length()) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCOMPRESSIBLE_VIDEO, locale))
                        .setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }

            String compessionInfo = messageBuilder.getVideoCompressionInfoMessage(fileQueueItem.getSize(), result.length(),
                    userService.getLocaleOrDefault(fileQueueItem.getUserId()));

            return videoResultBuilder.build(fileQueueItem, fileQueueItem.getFirstFileFormat(), compessionInfo, result);
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

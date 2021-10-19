package ru.gadjini.telegram.converter.service.conversion.impl;

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
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
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

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private final FFmpegConversionContextPreparerChain conversionContextPreparer;

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public MakeVideoSquare(FFmpegDevice fFmpegDevice, LocalisationService localisationService,
                           UserService userService, FFprobeDevice fFprobeDevice,
                           FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                           FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                           FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory,
                           FFmpegCommandBuilderFactory commandBuilderChainFactory, VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.callbackHandlerFactory = callbackHandlerFactory;

        this.commandBuilderChain = commandBuilderChainFactory.hideBannerQuite();
        this.videoResultBuilder = videoResultBuilder;
        commandBuilderChain.setNext(commandBuilderChainFactory.input())
                .setNext(commandBuilderChainFactory.telegramVideoConversion())
                .setNext(commandBuilderChainFactory.videoConversion())
                .setNext(commandBuilderChainFactory.audioInVideoConversion())
                .setNext(commandBuilderChainFactory.subtitlesConversion())
                .setNext(commandBuilderChainFactory.webmQuality())
                .setNext(commandBuilderChainFactory.fastVideoConversion())
                .setNext(commandBuilderChainFactory.enableExperimentalFeatures())
                .setNext(commandBuilderChainFactory.synchronizeVideoTimestamp())
                .setNext(commandBuilderChainFactory.maxMuxingQueueSize())
                .setNext(commandBuilderChainFactory.output());

        this.conversionContextPreparer = contextPreparerChainFactory.telegramVideoContextPreparer();
        conversionContextPreparer.setNext(contextPreparerChainFactory.telegramVoiceContextPreparer())
                .setNext(contextPreparerChainFactory.subtitlesContextPreparer())
                .setNext(contextPreparerChainFactory.squareVideo());
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, TARGET_FORMAT.getExt());
        try {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());

            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams));
            validate(fileQueueItem, srcWhd);

            int size = Math.max(srcWhd.getHeight(), srcWhd.getWidth());
            FFmpegConversionContext conversionContext = new FFmpegConversionContext()
                    .streams(allStreams)
                    .input(file)
                    .output(result)
                    .putExtra(FFmpegConversionContext.SQUARE_SIZE, size);
            conversionContextPreparer.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);

            FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(fileQueueItem, srcWhd.getDuration(),
                    userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            fFmpegDevice.execute(command.toCmd(), callback);

            String caption = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SQUARE_CAPTION,
                    new Object[]{size + "x" + size}, userService.getLocaleOrDefault(fileQueueItem.getUserId()));

            return videoResultBuilder.build(fileQueueItem, fileQueueItem.getFirstFileFormat(), caption, result);
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private void validate(ConversionQueueItem fileQueueItem, FFprobeDevice.WHD srcWhd) {
        if (Objects.equals(srcWhd.getHeight(), srcWhd.getWidth())) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_ALREADY_SQUARE,
                    new Object[]{srcWhd.getWidth() + "x" + srcWhd.getHeight()}, userService.getLocaleOrDefault(fileQueueItem.getUserId())))
                    .setReplyToMessageId(fileQueueItem.getReplyToMessageId());
        }
    }
}

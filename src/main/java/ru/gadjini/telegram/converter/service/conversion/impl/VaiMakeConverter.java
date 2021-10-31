package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class VaiMakeConverter extends BaseAny2AnyConverter {

    private static final String TAG = "vaimake";

    public static final Format OUTPUT_FORMAT = Format.MP4;

    public static final int AUDIO_FILE_INDEX = 0;

    public static final int IMAGE_FILE_INDEX = 1;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGEAUDIO), List.of(Format.VMAKE)
    );

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private final FFmpegConversionContextPreparerChain contextPreparer;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private UserService userService;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public VaiMakeConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                            FFmpegProgressCallbackHandlerFactory callbackHandlerFactory, UserService userService,
                            FFmpegCommandBuilderFactory commandBuilderFactory,
                            FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory,
                            VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.userService = userService;

        this.contextPreparer = contextPreparerChainFactory.telegramVideoContextPreparer();
        this.videoResultBuilder = videoResultBuilder;
        contextPreparer.setNext(contextPreparerChainFactory.streamScaleContextPreparer());

        this.commandBuilderChain = commandBuilderFactory.singleLoop();
        commandBuilderChain.setNext(commandBuilderFactory.input())
                .setNext(commandBuilderFactory.singleFramerate())
                .setNext(commandBuilderFactory.input())
                .setNext(commandBuilderFactory.vaiMake())
                .setNext(commandBuilderFactory.videoConversion())
                .setNext(commandBuilderFactory.audioInVideoConversion())
                .setNext(commandBuilderFactory.webmQuality())
                .setNext(commandBuilderFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderFactory.output());
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem conversionQueueItem) {
        SmartTempFile downloadedAudio = conversionQueueItem.getDownloadedFiles().get(AUDIO_FILE_INDEX);
        SmartTempFile downloadedImage = conversionQueueItem.getDownloadedFiles().get(IMAGE_FILE_INDEX);

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                conversionQueueItem.getFirstFileId(), TAG, OUTPUT_FORMAT.getExt());

        try {
            List<FFprobeDevice.FFProbeStream> audioStreams = fFprobeDevice.getAllStreams(downloadedAudio.getAbsolutePath());
            List<FFprobeDevice.FFProbeStream> streams = new ArrayList<>();
            streams.addAll(audioStreams);
            streams.addAll(fFprobeDevice.getAllStreams(downloadedImage.getAbsolutePath()));
            FFmpegConversionContext conversionContext = new FFmpegConversionContext()
                    .streams(streams)
                    .input(downloadedImage)
                    .input(downloadedAudio)
                    .output(result)
                    .outputFormat(OUTPUT_FORMAT);
            contextPreparer.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);

            FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = callbackHandlerFactory
                    .createCallback(conversionQueueItem, audioStreams.iterator().next().getDuration(),
                            userService.getLocaleOrDefault(conversionQueueItem.getUserId()));
            fFmpegDevice.execute(command.toCmd(), callback);

            return videoResultBuilder.build(conversionQueueItem, OUTPUT_FORMAT, result);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}

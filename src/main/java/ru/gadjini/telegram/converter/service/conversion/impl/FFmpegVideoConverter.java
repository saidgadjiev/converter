package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegVideoConverter extends BaseAny2AnyConverter {

    private static final String TAG = "ffmpegvideo";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(MP4), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(_3GP), List.of(MP4, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(AVI), List.of(MP4, _3GP, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(FLV), List.of(MP4, _3GP, AVI, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(M4V), List.of(MP4, _3GP, AVI, FLV, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MKV), List.of(MP4, _3GP, AVI, FLV, M4V, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MOV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MPEG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MPG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MTS), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, VOB, WEBM, WMV, TS));
        put(List.of(VOB), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, WEBM, WMV, TS));
        put(List.of(WEBM), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, TS));
        put(List.of(WMV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, TS));
        put(List.of(TS), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, MP4));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private VideoResultBuilder videoResultBuilder;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private UserService userService;

    private FFmpegConversionContextPreparerChain conversionContextPreparer;

    private FFmpegCommandBuilderChain commandBuilderChain;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    @Autowired
    public FFmpegVideoConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                VideoResultBuilder videoResultBuilder,
                                FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                                UserService userService, FFmpegConversionContextPreparerChainFactory chainFactory,
                                FFmpegCommandBuilderFactory commandBuilderChainFactory,
                                FFmpegVideoStreamConversionHelper videoStreamConversionHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.videoResultBuilder = videoResultBuilder;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.userService = userService;
        this.videoStreamConversionHelper = videoStreamConversionHelper;

        this.commandBuilderChain = commandBuilderChainFactory.quiteInput();
        commandBuilderChain.setNext(commandBuilderChainFactory.simpleVideoStreamsConversionWithWebmQuality())
                .setNext(commandBuilderChainFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderChainFactory.output());

        this.conversionContextPreparer = chainFactory.videoConversionContextPreparer();
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
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(),
                TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            return doConvert(file, result, fileQueueItem, fileQueueItem.getTargetFormat(), true);
        } catch (CorruptedVideoException e) {
            tempFileService().delete(file);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    public ConversionResult doConvert(SmartTempFile file, SmartTempFile result,
                                      ConversionQueueItem fileQueueItem, Format targetFormat,
                                      boolean withProgress) throws InterruptedException {
        List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getStreams(file.getAbsolutePath(), FormatCategory.VIDEO);

        FFmpegConversionContext conversionContext = FFmpegConversionContext.from(file, result, targetFormat, allStreams);
        conversionContextPreparer.prepare(conversionContext);

        FFmpegCommand command = new FFmpegCommand();
        commandBuilderChain.prepareCommand(command, conversionContext);

        FFprobeDevice.WHD sourceWdh = fFprobeDevice.getWHD(file.getAbsolutePath(), videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
        FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = withProgress ? callbackHandlerFactory.createCallback(fileQueueItem,
                sourceWdh.getDuration(), userService.getLocaleOrDefault(fileQueueItem.getUserId())) : null;
        fFmpegDevice.execute(command.toCmd(), callback);

        return videoResultBuilder.build(fileQueueItem, result);
    }
}

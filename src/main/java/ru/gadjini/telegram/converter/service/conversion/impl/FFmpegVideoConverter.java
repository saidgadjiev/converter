package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
@SuppressWarnings("CPD-START")
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

    private FFmpegSubtitlesStreamConversionHelper fFmpegHelper;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private CaptionGenerator captionGenerator;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private UserService userService;

    @Autowired
    public FFmpegVideoConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                FFmpegSubtitlesStreamConversionHelper fFmpegHelper,
                                FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                                CaptionGenerator captionGenerator, FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                                UserService userService) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegHelper = fFmpegHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.captionGenerator = captionGenerator;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.userService = userService;
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
        List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
        FFmpegCommand commandBuilder = new FFmpegCommand();

        commandBuilder.hideBanner().quite().input(file.getAbsolutePath());
        if (targetFormat.canBeSentAsVideo()) {
            fFmpegVideoHelper.copyOrConvertVideoCodecsForTelegramVideo(commandBuilder, result, allStreams, targetFormat, fileQueueItem.getSize());
        } else {
            fFmpegVideoHelper.copyOrConvertVideoCodecs(commandBuilder, allStreams, targetFormat, result
            );
        }
        fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);

        FFmpegCommand baseCommand = new FFmpegCommand(commandBuilder);
        if (targetFormat.canBeSentAsVideo()) {
            videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
        } else {
            videoAudioConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, allStreams, result, targetFormat);
        }
        fFmpegHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, allStreams, result, targetFormat);

        if (WEBM.equals(targetFormat)) {
            commandBuilder.vp8QMinQMax();
        }
        commandBuilder.fastConversion().defaultOptions().out(result.getAbsolutePath());
        FFprobeDevice.WHD sourceWdh = fFprobeDevice.getWHD(file.getAbsolutePath(), 0);

        FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = withProgress ? callbackHandlerFactory.createCallback(fileQueueItem,
                sourceWdh.getDuration(), userService.getLocaleOrDefault(fileQueueItem.getUserId())) : null;
        fFmpegDevice.execute(commandBuilder.buildFullCommand(), callback);

        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), targetFormat.getExt());

        String caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource());
        if (targetFormat.canBeSentAsVideo()) {
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

            return new VideoResult(fileName, result, targetFormat, downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                    whd.getDuration(), targetFormat.supportsStreaming(), caption);
        } else {
            return new FileResult(fileName, result, downloadThumb(fileQueueItem), caption);
        }
    }
}

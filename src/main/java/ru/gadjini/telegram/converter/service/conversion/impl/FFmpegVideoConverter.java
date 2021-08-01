package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
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
        put(List.of(MP4), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(_3GP), List.of(MP4, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(AVI), List.of(MP4, _3GP, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(FLV), List.of(MP4, _3GP, AVI, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(M4V), List.of(MP4, _3GP, AVI, FLV, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(MKV), List.of(MP4, _3GP, AVI, FLV, M4V, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(MOV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(MPEG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(MPG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MTS, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(MTS), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, VOB, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(VOB), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, WEBM, WMV, TS, AVI_H263_PLUS));
        put(List.of(WEBM), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, TS, AVI_H263_PLUS));
        put(List.of(WMV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, TS, AVI_H263_PLUS));
        put(List.of(TS), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, MP4, AVI_H263_PLUS));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegSubtitlesStreamConversionHelper fFmpegHelper;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private CaptionGenerator captionGenerator;

    @Autowired
    public FFmpegVideoConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                FFmpegSubtitlesStreamConversionHelper fFmpegHelper,
                                FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                                CaptionGenerator captionGenerator) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegHelper = fFmpegHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.captionGenerator = captionGenerator;
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
            fFmpegVideoHelper.validateVideoIntegrity(file, result);
            return doConvert(file, result, fileQueueItem, fileQueueItem.getTargetFormat());
        } catch (CorruptedVideoException e) {
            tempFileService().delete(file);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    public ConversionResult doConvert(SmartTempFile file, SmartTempFile result, ConversionQueueItem fileQueueItem, Format targetFormat) throws InterruptedException {
        List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

        commandBuilder.hideBanner().quite().input(file.getAbsolutePath());
        if (targetFormat.canBeSentAsVideo()) {
            fFmpegVideoHelper.copyOrConvertVideoCodecsForTelegramVideo(commandBuilder, allStreams, targetFormat, fileQueueItem.getSize());
        } else {
            fFmpegVideoHelper.copyOrConvertVideoCodecs(commandBuilder, allStreams, targetFormat, result,
                    targetFormat == AVI_H263_PLUS ? FFmpegCommandBuilder.H263_PLUS_CODEC : null, fileQueueItem.getSize());
        }
        fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        if (targetFormat.canBeSentAsVideo()) {
            videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
        } else {
            videoAudioConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, allStreams, result, targetFormat);
        }
        if (targetFormat != AVI_H263_PLUS) {
            fFmpegHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, allStreams, result, targetFormat);
        }

        if (WEBM.equals(targetFormat)) {
            commandBuilder.vp8QualityOptions();
        }
        commandBuilder.fastConversion().defaultOptions().out(result.getAbsolutePath());
        fFmpegDevice.execute(commandBuilder.buildFullCommand());

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

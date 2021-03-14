package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
@SuppressWarnings("CPD-START")
public class FFmpegVideoFormatsConverter extends BaseAny2AnyConverter {

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

    private FFmpegHelper fFmpegHelper;

    private FFmpegVideoConversionHelper videoConversionHelper;

    @Autowired
    public FFmpegVideoFormatsConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                       FFmpegHelper fFmpegHelper, FFmpegVideoConversionHelper videoConversionHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegHelper = fFmpegHelper;
        this.videoConversionHelper = videoConversionHelper;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(),
                TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            return doConvert(file, result, fileQueueItem);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    public ConversionResult doConvert(SmartTempFile file, SmartTempFile out, ConversionQueueItem fileQueueItem) throws InterruptedException {
        List<FFprobeDevice.Stream> allStreams = videoConversionHelper.getStreamsForConversion(file);
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

        List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());
        for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); ++videoStreamIndex) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamIndex);
            commandBuilder.mapVideo(videoStreamIndex);
            if (videoConversionHelper.isCopyable(file, out, fileQueueItem.getTargetFormat(),
                    FFmpegCommandBuilder.VIDEO_STREAM_SPECIFIER, videoStreamIndex)) {
                commandBuilder.copyVideo(videoStreamIndex);
            } else {
                boolean convertibleToH264 = videoConversionHelper.isConvertibleToH264(file, out, videoStream,
                        videoStreamIndex, FFmpegCommandBuilder.EVEN_SCALE);
                if (videoConversionHelper.addFastestVideoCodec(commandBuilder, videoStream, videoStreamIndex,
                        convertibleToH264, FFmpegCommandBuilder.EVEN_SCALE)) {
                    videoConversionHelper.addVideoCodecByTargetFormat(commandBuilder, fileQueueItem.getTargetFormat(), videoStreamIndex);
                }
            }
        }
        videoConversionHelper.copyOrConvertAudioCodecs(commandBuilder, allStreams, file, out, fileQueueItem);
        fFmpegHelper.copyOrConvertSubtitlesCodecs(commandBuilder, allStreams, file, out, fileQueueItem);
        videoConversionHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getTargetFormat());
        commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
        commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);

        fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());

        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

        if (fileQueueItem.getTargetFormat().canBeSentAsVideo()) {
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(out.getAbsolutePath(), 0);

            return new VideoResult(fileName, out, fileQueueItem.getTargetFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                    whd.getDuration(), fileQueueItem.getTargetFormat().supportsStreaming());
        } else {
            return new FileResult(fileName, out, downloadThumb(fileQueueItem));
        }
    }
}

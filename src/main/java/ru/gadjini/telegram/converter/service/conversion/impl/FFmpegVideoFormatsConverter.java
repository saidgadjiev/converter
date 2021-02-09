package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.FFmpegHelper;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
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

    @Autowired
    public FFmpegVideoFormatsConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegHelper.removeExtraVideoStreams(allStreams);

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); ++videoStreamIndex) {
                commandBuilder.mapVideo(videoStreamIndex);
                if (isCopyable(file, out, fileQueueItem.getTargetFormat(), FFmpegCommandBuilder.VIDEO_STREAM_SPECIFIER, videoStreamIndex)) {
                    commandBuilder.copyVideo(videoStreamIndex);
                } else {
                    addVideoCodecOptions(commandBuilder, fileQueueItem.getTargetFormat(), videoStreamIndex);
                }
            }
            if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
                commandBuilder.mapAudio();
                List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                        .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                        .collect(Collectors.toList());
                for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                    if (isCopyable(file, out, fileQueueItem.getTargetFormat(), FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER, audioStreamIndex)) {
                        commandBuilder.copyAudio(audioStreamIndex);
                    } else {
                        addAudioCodecOptions(commandBuilder, fileQueueItem.getTargetFormat(), audioStreamIndex);
                    }
                }
            }
            addTargetFormatOptions(commandBuilder, fileQueueItem.getTargetFormat());
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
        } catch (Throwable e) {
            out.smartDelete();
            throw e;
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    private boolean isCopyable(SmartTempFile in, SmartTempFile out, Format targetFormat, String streamPrefix, int streamIndex) {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
        commandBuilder.map(streamPrefix, streamIndex).copy(streamPrefix);
        addTargetFormatOptions(commandBuilder, targetFormat);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    private void addTargetFormatOptions(FFmpegCommandBuilder commandBuilder, Format target) {
        if (target == MPEG || target == MPG) {
            commandBuilder.f(FFmpegCommandBuilder.MPEGTS_FORMAT);
        } else if (target == _3GP) {
            commandBuilder.ar("8000").ba("12.20k").ac("1").s("176x144");
        } else if (target == FLV) {
            commandBuilder.f(FFmpegCommandBuilder.FLV_FORMAT).ar("44100");
        } else if (target == MTS) {
            commandBuilder.r("30000/1001");
        }
    }

    private void addAudioCodecOptions(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == MTS) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.AC3_CODEC);
        }
    }

    private void addVideoCodecOptions(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == WEBM) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.VP8_CODEC);
        } else if (target == _3GP) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.H263_CODEC);
        } else if (target == MTS) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.H264_CODEC).bv(streamIndex, "21M");
        } else if (target == WMV) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.WMV2);
        }
    }
}

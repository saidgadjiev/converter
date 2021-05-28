package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class FFmpegVideoHelper {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegVideoHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public void validateVideoIntegrity(SmartTempFile in) throws InterruptedException {
        boolean validFile = fFmpegDevice.isValidFile(in.getAbsolutePath());

        if (!validFile) {
            throw new CorruptedVideoException();
        }
    }

    public boolean isVideoStreamsValidForTelegramVideo(List<FFprobeDevice.Stream> allStreams) {
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        return videoStreams.stream().allMatch(v -> FFmpegCommandBuilder.H264_CODEC.equals(v.getCodecName()));
    }

    public void copyOrConvertVideoCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                                         Format targetFormat) {
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());
        String scale = targetFormat == _3GP ? FFmpegCommandBuilder._3GP_SCALE : FFmpegCommandBuilder.EVEN_SCALE;

        int outCodecIndex = 0;
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamMapIndex);
            if (FFmpegVideoConversionHelper.isExtraVideoStream(videoStreams, videoStream)) {
                continue;
            }
            commandBuilder.mapVideo(videoStream.getInput(), videoStreamMapIndex);
            if (FFmpegCommandBuilder.H264_CODEC.equals(videoStream.getCodecName())) {
                commandBuilder.copyVideo(outCodecIndex);
            } else {
                commandBuilder.videoCodec(outCodecIndex, FFmpegCommandBuilder.H264_CODEC)
                        .filterVideo(outCodecIndex, scale);
            }
            ++outCodecIndex;
        }
    }

    public void convertVideoCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                                   Format targetFormat) {
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());
        String scale = targetFormat == _3GP ? FFmpegCommandBuilder._3GP_SCALE : FFmpegCommandBuilder.EVEN_SCALE;

        int outCodecIndex = 0;
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamMapIndex);
            if (FFmpegVideoConversionHelper.isExtraVideoStream(videoStreams, videoStream)) {
                continue;
            }
            commandBuilder.mapVideo(videoStream.getInput(), videoStreamMapIndex);
            commandBuilder.videoCodec(outCodecIndex, FFmpegCommandBuilder.H264_CODEC)
                    .filterVideo(outCodecIndex, scale);

            ++outCodecIndex;
        }
    }

    public void copyOrConvertVideoCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                         Format targetFormat, SmartTempFile result) throws InterruptedException {
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());
        String scale = targetFormat == _3GP ? FFmpegCommandBuilder._3GP_SCALE : FFmpegCommandBuilder.EVEN_SCALE;

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        int outCodecIndex = 0;
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamMapIndex);
            if (FFmpegVideoConversionHelper.isExtraVideoStream(videoStreams, videoStream)) {
                continue;
            }
            commandBuilder.mapVideo(videoStream.getInput(), videoStreamMapIndex);
            if (isCopyableVideoCodecs(baseCommand, result, targetFormat, videoStream.getInput(), videoStreamMapIndex)) {
                commandBuilder.copyVideo(outCodecIndex);
            } else {
                boolean convertibleToH264 = isConvertibleToH264(baseCommand, result,
                        videoStream.getInput(), videoStreamMapIndex, scale);
                if (!addFastestVideoCodec(commandBuilder, videoStream, outCodecIndex,
                        convertibleToH264, scale)) {
                    addVideoCodecByTargetFormat(commandBuilder, targetFormat, outCodecIndex);
                }
            }
            ++outCodecIndex;
        }
    }

    public void convertVideoCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                   Format targetFormat, SmartTempFile result) throws InterruptedException {
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());
        String scale = targetFormat == _3GP ? FFmpegCommandBuilder._3GP_SCALE : FFmpegCommandBuilder.EVEN_SCALE;

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        int outCodecIndex = 0;
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamMapIndex);
            if (FFmpegVideoConversionHelper.isExtraVideoStream(videoStreams, videoStream)) {
                continue;
            }
            commandBuilder.mapVideo(videoStream.getInput(), videoStreamMapIndex);

            boolean convertibleToH264 = isConvertibleToH264(baseCommand, result,
                    videoStream.getInput(), videoStreamMapIndex, scale);
            if (!addFastestVideoCodec(commandBuilder, videoStream, outCodecIndex,
                    convertibleToH264, scale)) {
                addVideoCodecByTargetFormat(commandBuilder, targetFormat, outCodecIndex);
            }
            ++outCodecIndex;
        }
    }

    private boolean isCopyableVideoCodecs(FFmpegCommandBuilder baseCommand, SmartTempFile out,
                                          Format targetFormat, Integer input, int videoStreamMapIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommand);

        commandBuilder.mapVideo(input, videoStreamMapIndex).copy(FFmpegCommandBuilder.VIDEO_STREAM_SPECIFIER);
        addVideoTargetFormatOptions(commandBuilder, targetFormat);
        commandBuilder.defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    public boolean isConvertibleToH264(FFmpegCommandBuilder baseCommand, SmartTempFile out,
                                       Integer input, int videoStreamMapIndex, String scale) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommand);
        commandBuilder.mapVideo(input, videoStreamMapIndex).videoCodec(FFmpegCommandBuilder.H264_CODEC).filterVideo(scale);
        commandBuilder.defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    public boolean addFastestVideoCodec(FFmpegCommandBuilder commandBuilder, FFprobeDevice.Stream videoStream,
                                        int videoStreamIndex, boolean convertibleToH264, String h264Scale) {
        if (StringUtils.isBlank(videoStream.getCodecName())) {
            return false;
        }
        if (convertibleToH264) {
            commandBuilder.videoCodec(videoStreamIndex, FFmpegCommandBuilder.H264_CODEC);
            if (StringUtils.isNotBlank(h264Scale)) {
                commandBuilder.filterVideo(videoStreamIndex, h264Scale);
            }
            return true;
        } else if (FFmpegCommandBuilder.VP9_CODEC.equals(videoStream.getCodecName())) {
            commandBuilder.videoCodec(videoStreamIndex, FFmpegCommandBuilder.VP8_CODEC);
            return true;
        } else if (FFmpegCommandBuilder.AV1_CODEC.equals(videoStream.getCodecName())) {
            commandBuilder.videoCodec(videoStreamIndex, FFmpegCommandBuilder.VP8_CODEC);
            return true;
        } else {
            return false;
        }
    }

    public void addVideoTargetFormatOptions(FFmpegCommandBuilder commandBuilder, Format target) {
        if (target == MPEG || target == MPG) {
            commandBuilder.f(FFmpegCommandBuilder.MPEGTS_FORMAT);
        } else if (target == _3GP) {
            commandBuilder.ar("8000").ba("12.20k").ac("1");
        } else if (target == FLV) {
            commandBuilder.f(FFmpegCommandBuilder.FLV_FORMAT).ar("44100");
        } else if (target == MTS) {
            commandBuilder.r("30000/1001");
        }
    }

    public void addVideoCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == WEBM) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.VP8_CODEC).bv(streamIndex, "2M");
        } else if (target == _3GP) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.H263_CODEC).filterVideo(streamIndex, FFmpegCommandBuilder._3GP_SCALE);
        } else if (target == MTS) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.H264_CODEC);
        } else if (target == WMV) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.WMV2);
        }
    }
}

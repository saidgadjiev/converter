package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class FFmpegVideoHelper {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegVideoHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public boolean isCopyableVideoCodecs(SmartTempFile in, SmartTempFile out, Format targetFormat, int streamIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

        commandBuilder.map(FFmpegCommandBuilder.VIDEO_STREAM_SPECIFIER, streamIndex).copy(FFmpegCommandBuilder.VIDEO_STREAM_SPECIFIER);
        addVideoTargetFormatOptions(commandBuilder, targetFormat);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    public boolean isConvertibleToH264(SmartTempFile in, SmartTempFile out, int videoStreamIndex, String scale) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
        commandBuilder.mapVideo(videoStreamIndex).videoCodec(FFmpegCommandBuilder.H264_CODEC).filterVideo(scale);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
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

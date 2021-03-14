package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class FFmpegVideoConversionHelper {

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegVideoConversionHelper(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    public List<FFprobeDevice.Stream> getStreamsForConversion(SmartTempFile file) throws InterruptedException {
        List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
        FFmpegVideoConversionHelper.removeExtraVideoStreams(allStreams);

        return allStreams;
    }

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                         SmartTempFile file, SmartTempFile out, ConversionQueueItem fileQueueItem) throws InterruptedException {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            commandBuilder.mapAudio();
            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            List<Integer> copyAudiosIndexes = new ArrayList<>();
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                if (isCopyable(file, out, fileQueueItem.getTargetFormat(), FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER, audioStreamIndex)) {
                    copyAudiosIndexes.add(audioStreamIndex);
                } else {
                    addAudioCodecByTargetFormat(commandBuilder, fileQueueItem.getTargetFormat(), audioStreamIndex);
                }
            }
            if (copyAudiosIndexes.size() == audioStreams.size()) {
                commandBuilder.copyAudio();
            } else {
                copyAudiosIndexes.forEach(commandBuilder::copyAudio);
            }
        }
    }

    public boolean isCopyable(SmartTempFile in, SmartTempFile out, Format targetFormat, String streamPrefix, int streamIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
        if (FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER.equals(streamPrefix)) {
            commandBuilder.mapVideo(0);
        }
        commandBuilder.map(streamPrefix, streamIndex).copy(streamPrefix);
        addVideoTargetFormatOptions(commandBuilder, targetFormat);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    public boolean isConvertibleToH264(SmartTempFile in, SmartTempFile out, FFprobeDevice.Stream videoStream,
                                       int videoStreamIndex, String scale) throws InterruptedException {
        if (FFmpegCommandBuilder.H264_CODEC.equals(videoStream.getCodecName())) {
            return true;
        }
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
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.VP8_CODEC);
        } else if (target == _3GP) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.H263_CODEC).filterVideo(streamIndex, FFmpegCommandBuilder._3GP_SCALE);
        } else if (target == MTS) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.H264_CODEC);
        } else if (target == WMV) {
            commandBuilder.videoCodec(streamIndex, FFmpegCommandBuilder.WMV2);
        }
    }

    private static void removeExtraVideoStreams(List<FFprobeDevice.Stream> streams) {
        FFprobeDevice.Stream videoStreamToCopy = streams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())
                        && isNotImageStream(s.getCodecName()))
                .findFirst().orElse(null);

        if (videoStreamToCopy == null) {
            return;
        }

        streams.removeIf(stream -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(stream.getCodecType())
                && videoStreamToCopy.getIndex() != stream.getIndex());
    }

    private static boolean isNotImageStream(String codecName) {
        return !"mjpeg".equals(codecName) && !"bmp".equals(codecName);
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == MTS) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.AC3_CODEC);
        }
    }
}

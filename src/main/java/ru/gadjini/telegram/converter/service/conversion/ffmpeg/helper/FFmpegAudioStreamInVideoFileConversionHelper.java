package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
@SuppressWarnings("CPD-START")
public class FFmpegAudioStreamInVideoFileConversionHelper {

    public static final String TELEGRAM_VIDEO_AUDIO_CODEC = FFmpegCommandBuilder.AAC_CODEC;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioStreamInVideoFileConversionHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public boolean isAudioStreamsValidForTelegramVideo(List<FFprobeDevice.FFProbeStream> allStreams) {
        List<FFprobeDevice.FFProbeStream> audioStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        return audioStreams.stream().allMatch(a -> TELEGRAM_VIDEO_AUDIO_CODEC.equals(a.getCodecName()));
    }

    public void copyOrConvertToTargetAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.FFProbeStream> allStreams,
                                                 boolean appendMapAudio) {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.FFProbeStream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            List<Integer> inputs = audioStreams.stream().map(FFprobeDevice.FFProbeStream::getInput).distinct().collect(Collectors.toList());
            int audioStreamIndex = 0;
            Map<Integer, Boolean> audioIndexes = new LinkedHashMap<>();

            for (Integer input : inputs) {
                if (appendMapAudio) {
                    commandBuilder.mapAudioInput(input);
                }
                List<FFprobeDevice.FFProbeStream> byInput = audioStreams.stream()
                        .filter(s -> input.equals(s.getInput()))
                        .collect(Collectors.toList());

                for (FFprobeDevice.FFProbeStream audioStream : byInput) {
                    if (isAudioStreamEquals(audioStream, targetAudioCodecName, targetAudioBitrate)) {
                        audioIndexes.put(audioStreamIndex, true);
                    } else {
                        audioIndexes.put(audioStreamIndex, false);
                    }
                    ++audioStreamIndex;
                }
            }

            if (audioIndexes.values().stream().allMatch(s -> s)) {
                commandBuilder.copyAudio();
            } else if (audioIndexes.values().stream().noneMatch(s -> s) && StringUtils.isNotBlank(targetAudioCodec)) {
                commandBuilder.audioCodec(targetAudioCodec);
            } else {
                audioIndexes.forEach((streamIndex, aBoolean) -> {
                    if (aBoolean) {
                        commandBuilder.copyAudio(streamIndex);
                    } else if (StringUtils.isNotBlank(targetAudioCodec)) {
                        commandBuilder.audioCodec(streamIndex, targetAudioCodec);
                    }
                });
            }
            if (targetAudioBitrate != null) {
                commandBuilder.audioBitrate(targetAudioBitrate);
            }
        }
    }

    public void addChannelMapFilter(FFmpegCommandBuilder commandBuilder, SmartTempFile out) throws InterruptedException {
        FFmpegCommandBuilder command = new FFmpegCommandBuilder(commandBuilder);
        command.out(out.getAbsolutePath());

        if (fFmpegDevice.isChannelMapError(command.buildFullCommand())) {
            commandBuilder.filterAudio("channelmap=channel_layout=5.1");
        }
    }

    public void addAudioTargetOptions(FFmpegCommandBuilder commandBuilder, Format target) {
        if (target.getAssociatedFormat() == AMR) {
            commandBuilder.ar("8000").ac("1");
        } else if (target.getAssociatedFormat().canBeSentAsVoice()) {
            commandBuilder.ar("48000");
        }
    }

    private void addAudioCodecOptions(FFmpegCommandBuilder commandBuilder, int streamIndex, Format target) {
        if (target.getAssociatedFormat().canBeSentAsVoice()) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.LIBOPUS);
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommandBuilder baseCommand, SmartTempFile out,
                                          Format targetFormat, Integer input, int streamMapIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommand);

        commandBuilder.mapAudio(input, streamMapIndex).copy(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER);
        addAudioTargetOptions(commandBuilder, targetFormat);
        commandBuilder.out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target) {
        if (target == MTS) {
            commandBuilder.audioCodec(FFmpegCommandBuilder.AC3_CODEC);
        } else if (target == WEBM) {
            commandBuilder.audioCodec(FFmpegCommandBuilder.LIBVORBIS);
        }
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == MTS) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.AC3_CODEC);
        } else if (target == WEBM) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.LIBVORBIS);
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommandBuilder baseCommandBuilder, SmartTempFile out, Integer input, int streamIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);

        commandBuilder.mapAudio(input, streamIndex).copy(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER);
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private boolean isAudioStreamEquals(FFprobeDevice.FFProbeStream stream, String audioCodecName, Long audioBitrate) {
        if (StringUtils.isNotBlank(audioCodecName) && !Objects.equals(audioCodecName, stream.getCodecName())) {
            return false;
        }

        if (audioBitrate != null && !Objects.equals(audioBitrate, stream.getBitRate())) {
            return false;
        }

        return true;
    }
}

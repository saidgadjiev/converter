package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MTS;

@Component
public class FFmpegAudioHelper {

    private static final String TELEGRAM_VIDEO_AUDIO_CODEC = FFmpegCommandBuilder.AAC_CODEC;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public boolean isAudioStreamsValidForTelegramVideo(List<FFprobeDevice.Stream> allStreams) {
        List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        return audioStreams.stream().allMatch(a -> TELEGRAM_VIDEO_AUDIO_CODEC.equals(a.getCodecName()));
    }

    public void copyOrConvertAudioCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams, boolean appendMapAudio) {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            Map<Integer, Boolean> audioIndexes = new LinkedHashMap<>();
            Integer input = audioStreams.iterator().next().getInput();
            if (appendMapAudio) {
                commandBuilder.mapAudioInput(input);
            }
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                FFprobeDevice.Stream audioStream = audioStreams.get(audioStreamIndex);
                if (TELEGRAM_VIDEO_AUDIO_CODEC.equals(audioStream.getCodecName())) {
                    audioIndexes.put(audioStreamIndex, true);
                } else {
                    audioIndexes.put(audioStreamIndex, false);
                }
            }
            if (audioIndexes.values().stream().allMatch(s -> s)) {
                commandBuilder.copyAudio();
            } else {
                audioIndexes.forEach((streamIndex, aBoolean) -> {
                    if (aBoolean) {
                        commandBuilder.copyAudio(streamIndex);
                    } else {
                        commandBuilder.audioCodec(streamIndex, TELEGRAM_VIDEO_AUDIO_CODEC);
                    }
                });
            }
        }
    }

    public void convertAudioCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams) {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            commandBuilder.mapAudio();
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                commandBuilder.audioCodec(audioStreamIndex, TELEGRAM_VIDEO_AUDIO_CODEC);
            }
        }
    }

    public void copyOrConvertAudioCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams) {
        copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams, true);
    }

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                         SmartTempFile out, Format targetFormat) throws InterruptedException {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            FFmpegCommandBuilder baseCommandToCheckCopyable = new FFmpegCommandBuilder(commandBuilder);

            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            List<Integer> copyAudiosIndexes = new ArrayList<>();
            Integer input = audioStreams.iterator().next().getInput();
            commandBuilder.mapAudioInput(input);
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                if (isCopyableAudioCodecs(baseCommandToCheckCopyable, out, input, audioStreamIndex)) {
                    copyAudiosIndexes.add(audioStreamIndex);
                } else {
                    addAudioCodecByTargetFormat(commandBuilder, targetFormat, audioStreamIndex);
                }
            }
            if (copyAudiosIndexes.size() == audioStreams.size()) {
                commandBuilder.copyAudio();
            } else {
                copyAudiosIndexes.forEach(commandBuilder::copyAudio);
            }
        }
    }

    public void convertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams, Format targetFormat) {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            commandBuilder.mapAudio();
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                addAudioCodecByTargetFormat(commandBuilder, targetFormat, audioStreamIndex);
            }
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommandBuilder baseCommandBuilder, SmartTempFile out, Integer input, int streamIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);

        commandBuilder.mapAudio(input, streamIndex).copy(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER);
        commandBuilder.defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == MTS) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.AC3_CODEC);
        }
    }
}

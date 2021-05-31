package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

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
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MTS;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

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
            } else if (audioIndexes.values().stream().noneMatch(s -> s)) {
                commandBuilder.audioCodec(TELEGRAM_VIDEO_AUDIO_CODEC);
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
            commandBuilder.mapAudio();
            commandBuilder.audioCodec(TELEGRAM_VIDEO_AUDIO_CODEC);
        }
    }

    public void copyOrConvertAudioCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams) {
        copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams, true);
    }

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder baseCommand, FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                         SmartTempFile out, Format targetFormat) throws InterruptedException {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            Map<Integer, Boolean> copyAudiosIndexes = new LinkedHashMap<>();
            Integer input = audioStreams.iterator().next().getInput();
            commandBuilder.mapAudioInput(input);
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                if (isCopyableAudioCodecs(baseCommand, out, input, audioStreamIndex)) {
                    copyAudiosIndexes.put(audioStreamIndex, true);
                } else {
                    copyAudiosIndexes.put(audioStreamIndex, false);
                }
            }
            if (copyAudiosIndexes.values().stream().allMatch(a -> a)) {
                commandBuilder.copyAudio();
            } else if (copyAudiosIndexes.values().stream().noneMatch(a -> a)) {
                addAudioCodecByTargetFormat(commandBuilder, targetFormat);
            } else {
                copyAudiosIndexes.forEach((integer, aBoolean) -> {
                    if (aBoolean) {
                        commandBuilder.copyAudio(integer);
                    } else {
                        addAudioCodecByTargetFormat(commandBuilder, targetFormat, integer);
                    }
                });
            }
        }
    }

    public void convertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams, Format targetFormat) {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            commandBuilder.mapAudio();
            addAudioCodecByTargetFormat(commandBuilder, targetFormat);
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommandBuilder baseCommandBuilder, SmartTempFile out, Integer input, int streamIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);

        commandBuilder.mapAudio(input, streamIndex).copy(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER);
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == MTS) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.AC3_CODEC);
        } else if (target == WEBM) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.LIBVORBIS);
        }
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target) {
        if (target == MTS) {
            commandBuilder.audioCodec(FFmpegCommandBuilder.AC3_CODEC);
        } else if (target == WEBM) {
            commandBuilder.audioCodec(FFmpegCommandBuilder.LIBVORBIS);
        }
    }
}

package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.*;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MTS;

@Component
public class FFmpegAudioHelper {

    private static final Set<String> TELEGRAM_VIDEO_AUDIO_CODECS = Set.of(FFmpegCommandBuilder.AAC_CODEC,
            FFmpegCommandBuilder.MP3);

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public boolean isAudioStreamsValidForTelegramVideo(List<FFprobeDevice.Stream> allStreams) {
        List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        return audioStreams.stream().allMatch(a -> TELEGRAM_VIDEO_AUDIO_CODECS.contains(a.getCodecName()));
    }

    public void copyOrConvertAudioCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams, boolean appendMapAudio) {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            Map<Integer, Boolean> audioIndexes = new LinkedHashMap<>();
            if (appendMapAudio) {
                commandBuilder.mapAudio();
            }
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                FFprobeDevice.Stream audioStream = audioStreams.get(audioStreamIndex);
                if (TELEGRAM_VIDEO_AUDIO_CODECS.contains(audioStream.getCodecName())) {
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
                        commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.MP3);
                    }
                });
            }
        }
    }

    public void copyOrConvertAudioCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams) {
        copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams, true);
    }

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                         SmartTempFile file, SmartTempFile out, Format targetFormat) throws InterruptedException {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            FFmpegCommandBuilder baseCommandToCheckCopyable = new FFmpegCommandBuilder(commandBuilder);

            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            List<Integer> copyAudiosIndexes = new ArrayList<>();
            commandBuilder.mapAudio();
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                if (isCopyableAudioCodecs(baseCommandToCheckCopyable, file, out, audioStreamIndex)) {
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

    private boolean isCopyableAudioCodecs(FFmpegCommandBuilder baseCommandBuilder, SmartTempFile in, SmartTempFile out,
                                          int streamIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);

        commandBuilder.map(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER, streamIndex).copy(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == MTS) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.AC3_CODEC);
        }
    }
}

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
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
@SuppressWarnings("CPD-START")
public class FFmpegAudioStreamInVideoFileConversionHelper {

    private static final String TELEGRAM_VIDEO_AUDIO_CODEC = FFmpegCommandBuilder.AAC_CODEC;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioStreamInVideoFileConversionHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public void convertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams, Format targetFormat) {
        List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                .filter(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))
                .collect(Collectors.toList());
        if (!audioStreams.isEmpty()) {
            commandBuilder.mapAudio();
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); audioStreamIndex++) {
                commandBuilder.keepAudioBitRate(audioStreamIndex, audioStreams.get(audioStreamIndex).getBitRate());
            }

            addAudioCodecByTargetFormat(commandBuilder, targetFormat);
        }
    }

    public void convertAudioCodecsForTelegramVideo(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams) {
        List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                .filter(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))
                .collect(Collectors.toList());
        if (!audioStreams.isEmpty()) {
            commandBuilder.mapAudio();

            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); audioStreamIndex++) {
                commandBuilder.keepAudioBitRate(audioStreamIndex, audioStreams.get(audioStreamIndex).getBitRate());
            }

            commandBuilder.audioCodec(TELEGRAM_VIDEO_AUDIO_CODEC);
        }
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
            List<Integer> inputs = audioStreams.stream().map(FFprobeDevice.Stream::getInput).distinct().collect(Collectors.toList());
            int audioStreamIndex = 0;
            Map<Integer, Boolean> audioIndexes = new LinkedHashMap<>();

            for (Integer input : inputs) {
                if (appendMapAudio) {
                    commandBuilder.mapAudioInput(input);
                }
                List<FFprobeDevice.Stream> byInput = audioStreams.stream()
                        .filter(s -> input.equals(s.getInput()))
                        .collect(Collectors.toList());

                for (FFprobeDevice.Stream audioStream: byInput) {
                    if (TELEGRAM_VIDEO_AUDIO_CODEC.equals(audioStream.getCodecName())) {
                        audioIndexes.put(audioStreamIndex, true);
                    } else {
                        audioIndexes.put(audioStreamIndex, false);
                    }
                    commandBuilder.keepAudioBitRate(audioStreamIndex, audioStream.getBitRate());
                    ++audioStreamIndex;
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
                commandBuilder.keepAudioBitRate(audioStreamIndex, audioStreams.get(audioStreamIndex).getBitRate());
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

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> audioStreams,
                                         Format targetFormat, SmartTempFile result) throws InterruptedException {
        copyOrConvertAudioCodecs(commandBuilder, audioStreams, targetFormat, result, null, null);
    }

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> audioStreams,
                                         Format targetFormat, SmartTempFile result, String targetCodecName, String targetCodec) throws InterruptedException {
        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        int outCodecIndex = 0;
        for (int audioStreamMapIndex = 0; audioStreamMapIndex < audioStreams.size(); ++audioStreamMapIndex) {
            FFprobeDevice.Stream audioStream = audioStreams.get(audioStreamMapIndex);

            commandBuilder.mapAudio(audioStream.getInput(), audioStreamMapIndex);
            if (StringUtils.isNotBlank(targetCodecName)) {
                if (targetCodecName.equals(audioStream.getCodecName())) {
                    commandBuilder.copyAudio(outCodecIndex);
                } else {
                    commandBuilder.audioCodec(outCodecIndex, targetCodec);
                }
            } else {
                if (isCopyableAudioCodecs(baseCommand, result, targetFormat, audioStream.getInput(), audioStreamMapIndex)) {
                    commandBuilder.copyAudio(outCodecIndex);
                } else {
                    addAudioCodecOptions(commandBuilder, outCodecIndex, targetFormat);
                }
            }
            commandBuilder.keepAudioBitRate(audioStreamMapIndex, audioStream.getBitRate());
            ++outCodecIndex;
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
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.OPUS);
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
}

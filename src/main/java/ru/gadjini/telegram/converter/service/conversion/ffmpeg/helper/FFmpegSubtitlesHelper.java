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

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegSubtitlesHelper {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegSubtitlesHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public void copyOrConvertOrIgnoreSubtitlesCodecs(FFmpegCommandBuilder baseCommand,
                                                     FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                                     SmartTempFile result, Format targetFormat) throws InterruptedException {
        if (!isSubtitlesSupported(targetFormat)) {
            return;
        }
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.Stream> subtitleStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            Integer input = subtitleStreams.iterator().next().getInput();
            Map<Integer, Boolean> copySubtitlesIndexes = new LinkedHashMap<>();
            Map<Integer, Integer> validSubtitlesIndexes = new LinkedHashMap<>();
            int ffmpegSubtitleStreamIndex = 0;
            int copyable = 0, convertable = 1, ignore = 2;
            Map<String, Integer> streamsCache = new HashMap<>();
            for (int subtitleStreamIndex = 0; subtitleStreamIndex < subtitleStreams.size(); ++subtitleStreamIndex) {
                FFprobeDevice.Stream subtitleStream = subtitleStreams.get(subtitleStreamIndex);
                if (streamsCache.containsKey(subtitleStream.getCodecName())) {
                    int state = streamsCache.get(subtitleStream.getCodecName());
                    if (state == convertable) {
                        int nextIndex = ffmpegSubtitleStreamIndex++;
                        validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                        copySubtitlesIndexes.put(nextIndex, false);
                    } else if (state == copyable) {
                        int nextIndex = ffmpegSubtitleStreamIndex++;
                        validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                        copySubtitlesIndexes.put(nextIndex, true);
                    }
                } else {
                    if (isSubtitlesCopyable(baseCommand, result, input, subtitleStreamIndex)) {
                        int nextIndex = ffmpegSubtitleStreamIndex++;
                        validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                        copySubtitlesIndexes.put(nextIndex, true);
                        streamsCache.put(subtitleStream.getCodecName(), copyable);
                    } else if (isSubtitlesConvertable(baseCommand, result, input, subtitleStreamIndex, targetFormat)) {
                        int nextIndex = ffmpegSubtitleStreamIndex++;
                        validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                        copySubtitlesIndexes.put(nextIndex, false);
                        streamsCache.put(subtitleStream.getCodecName(), convertable);
                    } else {
                        streamsCache.put(subtitleStream.getCodecName(), ignore);
                    }
                }
            }
            if (validSubtitlesIndexes.size() == subtitleStreams.size()) {
                commandBuilder.mapSubtitlesInput(input);
                if (copySubtitlesIndexes.values().stream().allMatch(a -> a)) {
                    commandBuilder.copySubtitles();
                } else if (copySubtitlesIndexes.values().stream().noneMatch(a -> a)) {
                    addSubtitlesCodec(commandBuilder, targetFormat);
                } else {
                    validSubtitlesIndexes.keySet().forEach((subtitlesIndex) -> {
                        if (copySubtitlesIndexes.get(subtitlesIndex)) {
                            commandBuilder.copySubtitles(subtitlesIndex);
                        } else {
                            addSubtitlesCodec(commandBuilder, subtitlesIndex, targetFormat);
                        }
                    });
                }
            } else {
                validSubtitlesIndexes.forEach((subtitlesIndex, mapIndex) -> {
                    commandBuilder.mapSubtitles(input, mapIndex);
                    if (copySubtitlesIndexes.get(subtitlesIndex)) {
                        commandBuilder.copySubtitles(subtitlesIndex);
                    } else {
                        addSubtitlesCodec(commandBuilder, subtitlesIndex, targetFormat);
                    }
                });
            }
        }
    }

    private boolean isSubtitlesCopyable(FFmpegCommandBuilder baseCommandBuilder,
                                        SmartTempFile out, Integer input, int index) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);
        commandBuilder.mapSubtitles(input, index).copySubtitles();
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private boolean isSubtitlesConvertable(FFmpegCommandBuilder baseCommandBuilder,
                                           SmartTempFile out, Integer input,
                                           int index, Format format) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);
        commandBuilder.mapSubtitles(input, index);
        addSubtitlesCodec(commandBuilder, format);
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private static boolean isSubtitlesSupported(Format format) {
        return Set.of(MP4, MOV, WEBM, MKV).contains(format);
    }

    private static void addSubtitlesCodec(FFmpegCommandBuilder commandBuilder, int index, Format format) {
        if (format == MP4 || format == MOV) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.MOV_TEXT_CODEC, index);
        } else if (format == WEBM) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.WEBVTT_CODEC, index);
        } else if (format == MKV) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.SRT_CODEC, index);
        }
    }

    private static void addSubtitlesCodec(FFmpegCommandBuilder commandBuilder, Format format) {
        if (format == MP4 || format == MOV) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.MOV_TEXT_CODEC);
        } else if (format == WEBM) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.WEBVTT_CODEC);
        } else if (format == MKV) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.SRT_CODEC);
        }
    }
}

package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.*;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegSubtitlesStreamConversionHelper {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegSubtitlesStreamConversionHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public void copyOrConvertOrIgnoreSubtitlesCodecs(FFmpegCommand baseCommand,
                                                     FFmpegCommand commandBuilder, List<FFprobeDevice.FFProbeStream> allStreams,
                                                     SmartTempFile result, Format targetFormat) throws InterruptedException {
        if (!isSubtitlesSupported(targetFormat)) {
            return;
        }
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.FFProbeStream.SUBTITLE_CODEC_TYPE.equals(stream.getCodecType()))) {
            List<FFprobeDevice.FFProbeStream> subtitleStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.FFProbeStream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());

            List<Integer> inputs = subtitleStreams.stream().map(FFprobeDevice.FFProbeStream::getInput).distinct().collect(Collectors.toList());
            int subtitleStreamIndex = 0;
            Map<Integer, Boolean> copySubtitlesIndexes = new LinkedHashMap<>();
            Map<Integer, Integer> validSubtitlesIndexes = new LinkedHashMap<>();
            int ffmpegSubtitleStreamIndex = 0;
            int copyable = 0, convertable = 1, ignore = 2;
            Map<String, Integer> streamsCache = new HashMap<>();

            List<Integer> validInputs = new ArrayList<>();
            for (Integer input : inputs) {
                List<FFprobeDevice.FFProbeStream> byInput = subtitleStreams.stream()
                        .filter(s -> input.equals(s.getInput()))
                        .collect(Collectors.toList());

                int prevValidStreamsSize = validSubtitlesIndexes.size();
                for (int i = 0; i < byInput.size(); ++i) {
                    FFprobeDevice.FFProbeStream subtitleStream = byInput.get(i);
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
                        if (isSubtitlesCopyable(baseCommand, result, input, i)) {
                            int nextIndex = ffmpegSubtitleStreamIndex++;
                            validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                            copySubtitlesIndexes.put(nextIndex, true);
                            streamsCache.put(subtitleStream.getCodecName(), copyable);
                        } else if (isSubtitlesConvertable(baseCommand, result, input, i, targetFormat)) {
                            int nextIndex = ffmpegSubtitleStreamIndex++;
                            validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                            copySubtitlesIndexes.put(nextIndex, false);
                            streamsCache.put(subtitleStream.getCodecName(), convertable);
                        } else {
                            streamsCache.put(subtitleStream.getCodecName(), ignore);
                        }
                    }
                    ++subtitleStreamIndex;
                }
                if (prevValidStreamsSize != validSubtitlesIndexes.size()) {
                    validInputs.add(input);
                }
            }

            for (Integer validInput : validInputs) {
                commandBuilder.mapSubtitlesInput(validInput);
            }
            if (validSubtitlesIndexes.size() == subtitleStreams.size()) {
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
                    if (copySubtitlesIndexes.get(subtitlesIndex)) {
                        commandBuilder.copySubtitles(subtitlesIndex);
                    } else {
                        addSubtitlesCodec(commandBuilder, subtitlesIndex, targetFormat);
                    }
                });
            }
        }
    }

    private boolean isSubtitlesCopyable(FFmpegCommand baseCommandBuilder,
                                        SmartTempFile out, Integer input, int index) throws InterruptedException {
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommandBuilder);
        commandBuilder.mapSubtitles(input, index).copySubtitles();
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private boolean isSubtitlesConvertable(FFmpegCommand baseCommandBuilder,
                                           SmartTempFile out, Integer input,
                                           int index, Format format) throws InterruptedException {
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommandBuilder);
        commandBuilder.mapSubtitles(input, index);
        addSubtitlesCodec(commandBuilder, format);
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private static boolean isSubtitlesSupported(Format format) {
        return Set.of(MP4, MOV, WEBM, MKV).contains(format);
    }

    private static void addSubtitlesCodec(FFmpegCommand commandBuilder, int index, Format format) {
        if (format == MP4 || format == MOV) {
            commandBuilder.subtitlesCodec(FFmpegCommand.MOV_TEXT_CODEC, index);
        } else if (format == WEBM) {
            commandBuilder.subtitlesCodec(FFmpegCommand.WEBVTT_CODEC, index);
        } else if (format == MKV) {
            commandBuilder.subtitlesCodec(FFmpegCommand.SRT_CODEC, index);
        }
    }

    private static void addSubtitlesCodec(FFmpegCommand commandBuilder, Format format) {
        if (format == MP4 || format == MOV) {
            commandBuilder.subtitlesCodec(FFmpegCommand.MOV_TEXT_CODEC);
        } else if (format == WEBM) {
            commandBuilder.subtitlesCodec(FFmpegCommand.WEBVTT_CODEC);
        } else if (format == MKV) {
            commandBuilder.subtitlesCodec(FFmpegCommand.SRT_CODEC);
        }
    }
}

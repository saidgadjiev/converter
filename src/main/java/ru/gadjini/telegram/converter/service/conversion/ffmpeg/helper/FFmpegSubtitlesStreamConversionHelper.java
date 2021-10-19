package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
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

    public void copyOrConvertOrIgnoreSubtitlesCodecs(FFmpegCommand baseCommand, FFmpegCommand commandBuilder,
                                                     FFmpegConversionContext conversionContext) throws InterruptedException {
        if (!isSubtitlesSupported(conversionContext.outputFormat())) {
            return;
        }
        List<FFprobeDevice.FFProbeStream> subtitleStreams = conversionContext.subtitleStreams();
        if (subtitleStreams.size() > 0) {
            List<Integer> inputs = subtitleStreams.stream().map(FFprobeDevice.FFProbeStream::getInput).distinct().collect(Collectors.toList());
            int subtitleStreamIndex = 0;
            Map<Integer, Boolean> copySubtitles = new LinkedHashMap<>();
            Map<Integer, Map.Entry<Integer, FFprobeDevice.FFProbeStream>> validSubtitles = new LinkedHashMap<>();
            int ffmpegSubtitleStreamIndex = 0;
            int copyable = 0, convertable = 1, ignore = 2;
            Map<String, Integer> streamsCache = new HashMap<>();

            List<Integer> validInputs = new ArrayList<>();
            for (Integer input : inputs) {
                List<FFprobeDevice.FFProbeStream> byInput = subtitleStreams.stream()
                        .filter(s -> input.equals(s.getInput()))
                        .collect(Collectors.toList());

                int prevValidStreamsSize = validSubtitles.size();
                for (int i = 0; i < byInput.size(); ++i) {
                    FFprobeDevice.FFProbeStream subtitleStream = byInput.get(i);
                    if (streamsCache.containsKey(subtitleStream.getCodecName())) {
                        int state = streamsCache.get(subtitleStream.getCodecName());
                        if (state == convertable) {
                            int nextIndex = ffmpegSubtitleStreamIndex++;
                            validSubtitles.put(nextIndex, Map.entry(subtitleStreamIndex, subtitleStream));
                            copySubtitles.put(nextIndex, false);
                        } else if (state == copyable) {
                            int nextIndex = ffmpegSubtitleStreamIndex++;
                            validSubtitles.put(nextIndex, Map.entry(subtitleStreamIndex, subtitleStream));
                            copySubtitles.put(nextIndex, true);
                        }
                    } else {
                        if (isSubtitlesCopyable(baseCommand, conversionContext.output(), input, i)) {
                            int nextIndex = ffmpegSubtitleStreamIndex++;
                            validSubtitles.put(nextIndex, Map.entry(subtitleStreamIndex, subtitleStream));
                            copySubtitles.put(nextIndex, true);
                            streamsCache.put(subtitleStream.getCodecName(), copyable);
                        } else if (isSubtitlesConvertable(baseCommand, conversionContext.output(), input, i)) {
                            int nextIndex = ffmpegSubtitleStreamIndex++;
                            validSubtitles.put(nextIndex, Map.entry(subtitleStreamIndex, subtitleStream));
                            copySubtitles.put(nextIndex, false);
                            streamsCache.put(subtitleStream.getCodecName(), convertable);
                        } else {
                            streamsCache.put(subtitleStream.getCodecName(), ignore);
                        }
                    }
                    ++subtitleStreamIndex;
                }
                if (prevValidStreamsSize != validSubtitles.size()) {
                    validInputs.add(input);
                }
            }

            for (Integer validInput : validInputs) {
                commandBuilder.mapSubtitlesInput(validInput);
            }
            if (copySubtitles.values().stream().allMatch(a -> a)) {
                commandBuilder.copySubtitles();
            } else {
                boolean sameCodec = subtitleStreams.stream().map(FFprobeDevice.FFProbeStream::getTargetCodecName)
                        .filter(StringUtils::isNotBlank).distinct().count() == 1;
                if (sameCodec && copySubtitles.values().stream().noneMatch(a -> a)) {
                    subtitleStreams.stream().findFirst().ifPresent(ffProbeStream -> {
                        commandBuilder.subtitlesCodec(ffProbeStream.getTargetCodecName());
                    });
                } else {
                    validSubtitles.forEach((integer, entry) -> {
                        if (copySubtitles.get(integer)) {
                            commandBuilder.copySubtitles(integer);
                        } else {
                            commandBuilder.subtitlesCodec(entry.getValue().getTargetCodecName(), entry.getKey());
                        }
                    });
                }
            }
        }
    }

    private boolean isSubtitlesCopyable(FFmpegCommand baseCommandBuilder,
                                        SmartTempFile out, Integer input, int index) throws InterruptedException {
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommandBuilder);
        commandBuilder.mapSubtitles(input, index).copySubtitles();
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }

    private boolean isSubtitlesConvertable(FFmpegCommand baseCommandBuilder,
                                           SmartTempFile out, Integer input,
                                           int index) throws InterruptedException {
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommandBuilder);
        commandBuilder.mapSubtitles(input, index);
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }

    private static boolean isSubtitlesSupported(Format format) {
        return Set.of(MP4, MOV, WEBM, MKV).contains(format);
    }
}

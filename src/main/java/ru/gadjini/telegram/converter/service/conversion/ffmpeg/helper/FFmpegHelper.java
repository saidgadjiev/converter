package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class FFmpegHelper {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public void validateVideoIntegrity(SmartTempFile in) throws InterruptedException {
        boolean validFile = fFmpegDevice.isValidFile(in.getAbsolutePath());

        if (!validFile) {
            throw new CorruptedVideoException();
        }
    }

    public void copyOrConvertOrIgnoreSubtitlesCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                                     SmartTempFile file, SmartTempFile result, ConversionQueueItem fileQueueItem) throws InterruptedException {
        if (!isSubtitlesSupported(fileQueueItem.getTargetFormat())) {
            return;
        }
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(stream.getCodecType()))) {
            FFmpegCommandBuilder baseCommandToCheckCopyable = new FFmpegCommandBuilder(commandBuilder);

            List<FFprobeDevice.Stream> subtitleStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            Map<Integer, Boolean> copySubtitlesIndexes = new LinkedHashMap<>();
            Map<Integer, Integer> validSubtitlesIndexes = new LinkedHashMap<>();
            int ffmpegSubtitleStreamIndex = 0;
            for (int subtitleStreamIndex = 0; subtitleStreamIndex < subtitleStreams.size(); ++subtitleStreamIndex) {
                if (isSubtitlesCopyable(baseCommandToCheckCopyable, file, result, subtitleStreamIndex)) {
                    int nextIndex = ffmpegSubtitleStreamIndex++;
                    validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                    copySubtitlesIndexes.put(nextIndex, true);
                } else if (isSubtitlesConvertable(commandBuilder, file, result, subtitleStreamIndex, fileQueueItem.getTargetFormat())) {
                    int nextIndex = ffmpegSubtitleStreamIndex++;
                    validSubtitlesIndexes.put(nextIndex, subtitleStreamIndex);
                    copySubtitlesIndexes.put(nextIndex, false);
                }
            }
            if (validSubtitlesIndexes.size() == subtitleStreams.size()) {
                if (copySubtitlesIndexes.values().stream().allMatch(a -> a)) {
                    commandBuilder.mapSubtitles().copySubtitles();
                } else {
                    commandBuilder.mapSubtitles();
                    FFmpegHelper.addSubtitlesCodec(commandBuilder, fileQueueItem.getTargetFormat());
                }
            } else {
                validSubtitlesIndexes.forEach((subtitlesIndex, mapIndex) -> {
                    commandBuilder.mapSubtitles(mapIndex);
                    if (copySubtitlesIndexes.get(subtitlesIndex)) {
                        commandBuilder.copySubtitles(subtitlesIndex);
                    } else {
                        FFmpegHelper.addSubtitlesCodec(commandBuilder, subtitlesIndex, fileQueueItem.getTargetFormat());
                    }
                });
            }
        }
    }

    private boolean isSubtitlesCopyable(FFmpegCommandBuilder baseCommandBuilder, SmartTempFile in,
                                        SmartTempFile out, int index) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);
        commandBuilder.mapSubtitles(index).copySubtitles();

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    private boolean isSubtitlesConvertable(FFmpegCommandBuilder baseCommandBuilder, SmartTempFile in,
                                           SmartTempFile out, int index, Format format) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);
        commandBuilder.mapSubtitles(index);
        FFmpegHelper.addSubtitlesCodec(commandBuilder, format);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
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

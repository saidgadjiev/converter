package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Set;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class FFmpegHelper {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public void copyOrConvertSubtitlesCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                             SmartTempFile file, SmartTempFile result, ConversionQueueItem fileQueueItem) throws InterruptedException {
        if (allStreams.stream().anyMatch(s -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))) {
            if (isSubtitlesCopyable(file, result)) {
                commandBuilder.mapSubtitles().copySubtitles();
            } else if (FFmpegHelper.isSubtitlesSupported(fileQueueItem.getTargetFormat())) {
                commandBuilder.mapSubtitles();
                FFmpegHelper.addSubtitlesCodec(commandBuilder, fileQueueItem.getTargetFormat());
            }
        }
    }

    private boolean isSubtitlesCopyable(SmartTempFile in, SmartTempFile out) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
        commandBuilder.mapSubtitles().copySubtitles();

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    private static boolean isSubtitlesSupported(Format format) {
        return Set.of(MP4, MOV, WEBM, MKV).contains(format);
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

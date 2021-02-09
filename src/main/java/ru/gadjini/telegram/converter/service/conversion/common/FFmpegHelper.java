package ru.gadjini.telegram.converter.service.conversion.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public boolean isSubtitlesCopyable(SmartTempFile in, SmartTempFile out) {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
        commandBuilder.mapSubtitles().copySubtitles();

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    public static boolean isSubtitlesSupported(Format format) {
        return Set.of(MP4, MOV, WEBM, MKV).contains(format);
    }

    public static void addSubtitlesCodec(FFmpegCommandBuilder commandBuilder, Format format) {
        if (format == MP4 || format == MOV) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.MOV_TEXT_CODEC);
        } else if (format == WEBM) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.WEBVTT_CODEC);
        } else if (format == MKV) {
            commandBuilder.subtitlesCodec(FFmpegCommandBuilder.SRT_CODEC);
        }
    }

    public static void removeExtraVideoStreams(List<FFprobeDevice.Stream> streams) {
        FFprobeDevice.Stream videoStreamToCopy = streams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())
                        && FFmpegHelper.isNotImageStream(s.getCodecName()))
                .findFirst().orElse(null);

        if (videoStreamToCopy == null) {
            return;
        }

        streams.removeIf(stream -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(stream.getCodecType())
                && videoStreamToCopy.getIndex() != stream.getIndex());
    }

    public static boolean isNotImageStream(String codecName) {
        return !"mjpeg".equals(codecName) && !"bmp".equals(codecName);
    }

    public static String[] getAudioOptions(Format target) {
        if (target == AMR) {
            return new String[]{
                    "-ar", "8000", "-ac", "1"
            };
        }
        if (target == OGG) {
            return new String[]{
                    "-c:a", "libvorbis", "-q:a", "4"
            };
        }
        return new String[0];
    }
}

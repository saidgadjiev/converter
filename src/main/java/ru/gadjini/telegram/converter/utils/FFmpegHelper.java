package ru.gadjini.telegram.converter.utils;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.AMR;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.OGG;

public class FFmpegHelper {

    private FFmpegHelper() {

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

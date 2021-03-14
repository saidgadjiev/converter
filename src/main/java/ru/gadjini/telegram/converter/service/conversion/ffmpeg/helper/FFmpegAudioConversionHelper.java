package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.AMR;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.OGG;

public class FFmpegAudioConversionHelper {

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

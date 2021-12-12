package ru.gadjini.telegram.converter.service.conversion.codec;

import org.apache.commons.lang3.StringUtils;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;

public class AudioCodecHelper {

    private AudioCodecHelper() {

    }

    public static String getCodec(String codecName) {
        if (StringUtils.isBlank(codecName)) {
            return null;
        }
        switch (codecName) {
            case "aac":
                return FFmpegCommand.AAC_CODEC;
            case "opus":
                return FFmpegCommand.LIBOPUS;
            case "vorbis":
                return FFmpegCommand.LIBVORBIS;
            case "wmapro":
                return "wmav2";
        }

        return codecName;
    }
}

package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.AMR;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.OGG;

public class FFmpegAudioConversionHelper {

    public static void addAudioOptions(Format target, FFmpegCommandBuilder commandBuilder) {
        if (target == AMR) {
            commandBuilder.ar("8000").ac("1");
        } else if (target == OGG) {
            commandBuilder.audioCodec(FFmpegCommandBuilder.VORBIS).qa("4");
        }
    }
}

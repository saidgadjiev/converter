package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

import static ru.gadjini.telegram.converter.service.conversion.impl.FFmpegAudioCompressConverter.MP3_FREQUENCY_44;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MP3;

public class FFmpegAudioCompressionCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        if (MP3.equals(conversionContext.outputFormat())) {
            String frequency = conversionContext.getExtra(FFmpegConversionContext.FREQUENCY);
            command.ar(normalizeFrequency(frequency));
        }
        super.prepareCommand(command, conversionContext);
    }

    private String normalizeFrequency(String frequency) {
        return MP3_FREQUENCY_44.equals(frequency) ? "44100" : "22050";
    }
}

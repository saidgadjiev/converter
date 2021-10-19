package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class FFmpegWebmQualityCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        if (conversionContext.outputFormat() == Format.WEBM) {
            command.qmin("0").qmax("40");
        }

        super.prepareCommand(command, conversionContext);
    }
}

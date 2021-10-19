package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

public class FFmpegInputCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        for (SmartTempFile input : conversionContext.getInputs()) {
            command.input(input.getAbsolutePath());
        }

        super.prepareCommand(command, conversionContext);
    }
}

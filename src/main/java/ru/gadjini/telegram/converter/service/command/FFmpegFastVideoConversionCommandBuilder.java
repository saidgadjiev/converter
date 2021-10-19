package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegFastVideoConversionCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        command.preset(FFmpegCommand.PRESET_VERY_FAST);

        command.speed("16");

        super.prepareCommand(command, conversionContext);
    }
}

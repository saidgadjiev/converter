package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVideoConversionMaxMuxingQueueSizeCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        command.maxMuxingQueueSize("9999");

        super.prepareCommand(command, conversionContext);
    }
}

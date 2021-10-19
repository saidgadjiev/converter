package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegStreamDurationCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        long duration = conversionContext.getExtra(FFmpegConversionContext.STREAM_DURATION);

        command.t(duration);

        super.prepareCommand(command, conversionContext);
    }
}

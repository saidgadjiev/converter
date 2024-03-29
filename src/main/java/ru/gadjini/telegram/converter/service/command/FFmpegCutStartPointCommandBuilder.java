package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegCutStartPointCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        String startPoint = conversionContext.getExtra(FFmpegConversionContext.CUT_START_POINT);

        command.ss(startPoint);

        super.prepareCommand(command, conversionContext);
    }
}

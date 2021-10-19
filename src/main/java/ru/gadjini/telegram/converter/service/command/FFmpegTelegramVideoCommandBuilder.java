package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegTelegramVideoCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        command.pixFmt(FFmpegCommand.YUV_420_P);

        super.prepareCommand(command, conversionContext);
    }
}

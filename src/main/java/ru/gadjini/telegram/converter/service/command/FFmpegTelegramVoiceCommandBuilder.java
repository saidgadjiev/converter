package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegTelegramVoiceCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        if (conversionContext.outputFormat().canBeSentAsVoice()) {
            command.ar("48000");
        }

        super.prepareCommand(command, conversionContext);
    }
}

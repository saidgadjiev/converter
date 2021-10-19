package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegAudioBassBoostCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        String bassBoost = conversionContext.getExtra(FFmpegConversionContext.BASS_BOOST);
        command.af("bass=g=" + bassBoost);

        super.prepareCommand(command, conversionContext);
    }
}

package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegMapAndCopyAudioCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        Integer audioIndex = conversionContext.getExtra(FFmpegConversionContext.MAP_AUDIO_INDEX);

        if (audioIndex == null) {
            command.mapAudio();
        } else {
            command.mapAudio(audioIndex);
        }
        command.copyAudio();

        super.prepareCommand(command, conversionContext);
    }
}

package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegAudioCodecsConvertCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegAudioStreamConversionHelper audioStreamConversionHelper;

    public FFmpegAudioCodecsConvertCommandBuilder(FFmpegAudioStreamConversionHelper audioStreamConversionHelper) {
        this.audioStreamConversionHelper = audioStreamConversionHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        audioStreamConversionHelper.copyOrConvertAudioCodecs(command, conversionContext);

        super.prepareCommand(command, conversionContext);
    }
}

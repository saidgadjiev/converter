package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVideoConversionAudioChannelMapFilterCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    public FFmpegVideoConversionAudioChannelMapFilterCommandBuilder(FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper) {
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        audioStreamInVideoFileConversionHelper.addChannelMapFilter(command, conversionContext.output());

        super.prepareCommand(command, conversionContext);
    }
}

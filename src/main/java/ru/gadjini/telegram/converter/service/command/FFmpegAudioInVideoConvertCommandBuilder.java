package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegAudioInVideoConvertCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    public FFmpegAudioInVideoConvertCommandBuilder(FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper) {
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        audioStreamInVideoFileConversionHelper.copyOrConvertAudioCodecs(command, conversionContext);

        super.prepareCommand(command, conversionContext);
    }
}

package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegAudioCoverCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegAudioStreamConversionHelper fFmpegAudioHelper;

    public FFmpegAudioCoverCommandBuilder(FFmpegAudioStreamConversionHelper fFmpegAudioHelper) {
        this.fFmpegAudioHelper = fFmpegAudioHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        fFmpegAudioHelper.addCopyableCoverArtOptions(conversionContext.getInput(), conversionContext.output(), command);

        super.prepareCommand(command, conversionContext);
    }
}

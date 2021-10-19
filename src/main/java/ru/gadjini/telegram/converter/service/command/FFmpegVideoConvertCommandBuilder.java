package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVideoConvertCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    public FFmpegVideoConvertCommandBuilder(FFmpegVideoStreamConversionHelper videoStreamConversionHelper) {
        this.videoStreamConversionHelper = videoStreamConversionHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        videoStreamConversionHelper.copyOrConvertVideoCodecs(command, conversionContext);

        super.prepareCommand(command, conversionContext);
    }
}

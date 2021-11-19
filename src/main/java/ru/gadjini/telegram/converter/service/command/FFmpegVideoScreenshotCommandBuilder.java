package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVideoScreenshotCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    public FFmpegVideoScreenshotCommandBuilder(FFmpegVideoStreamConversionHelper fFmpegVideoHelper) {
        this.fFmpegVideoHelper = fFmpegVideoHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        command.mapVideo(fFmpegVideoHelper.getFirstVideoStreamIndex(conversionContext.streams())).vframes("1").qv("2");

        super.prepareCommand(command, conversionContext);
    }
}

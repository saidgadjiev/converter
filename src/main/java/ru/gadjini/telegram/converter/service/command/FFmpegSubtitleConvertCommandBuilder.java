package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegSubtitleConvertCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper;

    public FFmpegSubtitleConvertCommandBuilder(FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper) {
        this.subtitlesStreamConversionHelper = subtitlesStreamConversionHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        subtitlesStreamConversionHelper.copyOrConvertOrIgnoreSubtitlesCodecs(new FFmpegCommand(command), command,
                conversionContext);

        super.prepareCommand(command, conversionContext);
    }
}

package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVaiMakeCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFprobeDevice fFprobeDevice;

    public FFmpegVaiMakeCommandBuilder(FFprobeDevice fFprobeDevice) {
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        long durationInSeconds = fFprobeDevice.getDurationInSeconds(conversionContext.getInputs().get(1).getAbsolutePath());
        command.tune(FFmpegCommand.TUNE_STILLIMAGE).t(durationInSeconds);

        super.prepareCommand(command, conversionContext);
    }
}

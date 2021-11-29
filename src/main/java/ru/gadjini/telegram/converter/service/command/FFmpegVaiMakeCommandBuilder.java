package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVaiMakeCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFprobeDevice fFprobeDevice;

    private FFmpegCommandBuilderChain audioInVideoConvertCommandBuilder;

    public FFmpegVaiMakeCommandBuilder(FFprobeDevice fFprobeDevice, FFmpegCommandBuilderChain audioInVideoConvertCommandBuilder) {
        this.fFprobeDevice = fFprobeDevice;
        this.audioInVideoConvertCommandBuilder = audioInVideoConvertCommandBuilder;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        long durationInSeconds = fFprobeDevice.getDurationInSeconds(conversionContext.getInputs().get(1).getAbsolutePath());
        command.mapVideo(0, 0).videoCodec(FFmpegCommand.H264_CODEC)
                .vf(FFmpegCommand.EVEN_SCALE);

        audioInVideoConvertCommandBuilder.prepareCommand(command, conversionContext);

        command.tune(FFmpegCommand.TUNE_STILLIMAGE).t(durationInSeconds);

        super.prepareCommand(command, conversionContext);
    }
}

package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;

public class VaiMakeConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        conversionContext.videoStreams().forEach(v -> {
            v.setTargetCodecName(FFmpegCommand.H264_CODEC);
            v.setTargetScale(FFmpegCommand.EVEN_SCALE);
        });

        super.prepare(conversionContext);
    }
}

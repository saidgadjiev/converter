package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class FFmpegVideoCompressionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    private static final String SCALE = "scale=-2:ceil(ih/3)*2";

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            stream.setTargetScale(SCALE);
        }
        conversionContext.setUseCrf(true);

        super.prepare(conversionContext);
    }
}

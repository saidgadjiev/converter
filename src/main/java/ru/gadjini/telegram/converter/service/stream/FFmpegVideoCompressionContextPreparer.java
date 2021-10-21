package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class FFmpegVideoCompressionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            stream.setTargetBitrate((int) (stream.getBitRate() * 0.7));
        }

        super.prepare(conversionContext);
    }
}

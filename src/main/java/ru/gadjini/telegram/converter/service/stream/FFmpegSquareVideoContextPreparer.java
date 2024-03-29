package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class FFmpegSquareVideoContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        int size = conversionContext.getExtra(FFmpegConversionContext.SQUARE_SIZE);
        size = size % 2 == 1 ? (size / 2) * 2 : size;
        String scale = "crop=" + size + ":" + size;

        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            stream.setTargetScale(scale);
        }

        super.prepare(conversionContext);
    }
}

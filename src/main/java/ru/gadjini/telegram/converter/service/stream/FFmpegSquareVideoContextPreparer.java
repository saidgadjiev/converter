package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class FFmpegSquareVideoContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        int size = conversionContext.getExtra(FFmpegConversionContext.SQUARE_SIZE);
        String crop = "(" + size + "/2)*2";
        String scale = "crop=" + crop + ":" + crop;

        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            stream.setTargetScale(scale);
        }

        super.prepare(conversionContext);
    }
}

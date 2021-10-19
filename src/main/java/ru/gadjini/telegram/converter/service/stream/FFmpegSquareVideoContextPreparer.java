package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class FFmpegSquareVideoContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        int size = conversionContext.getExtra(FFmpegConversionContext.SQUARE_SIZE);
        String scale = "scale='iw:ih':force_original_aspect_ratio=decrease,pad=" + size + ":" + size + ":(ow-iw)/2:(oh-ih)/2";

        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            stream.setTargetScale(scale);
        }

        super.prepare(conversionContext);
    }
}

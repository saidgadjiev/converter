package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class FFmpegAudioCompressionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        Integer bitrate = conversionContext.getExtra(FFmpegConversionContext.AUDIO_BITRATE);
        for (FFprobeDevice.FFProbeStream stream : conversionContext.audioStreams()) {
            stream.setTargetBitrate(bitrate);
        }

        super.prepare(conversionContext);
    }
}

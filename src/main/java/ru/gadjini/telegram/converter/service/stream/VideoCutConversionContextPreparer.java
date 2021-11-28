package ru.gadjini.telegram.converter.service.stream;

public class VideoCutConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        conversionContext.videoStreams().forEach(v -> v.setDontCopy(true));
        conversionContext.audioStreams().forEach(f -> f.setDontCopy(true));

        super.prepare(conversionContext);
    }
}

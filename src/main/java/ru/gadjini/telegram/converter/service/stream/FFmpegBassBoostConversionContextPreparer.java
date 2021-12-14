package ru.gadjini.telegram.converter.service.stream;

public class FFmpegBassBoostConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        conversionContext.useStaticAudioFilter();
        super.prepare(conversionContext);
    }
}

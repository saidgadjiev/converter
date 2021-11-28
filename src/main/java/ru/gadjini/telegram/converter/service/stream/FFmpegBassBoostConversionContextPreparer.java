package ru.gadjini.telegram.converter.service.stream;

public class FFmpegBassBoostConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        conversionContext.useStaticVideoFilter();
        super.prepare(conversionContext);
    }
}

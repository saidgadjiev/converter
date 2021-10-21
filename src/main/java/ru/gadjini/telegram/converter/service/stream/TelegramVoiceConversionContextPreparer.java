package ru.gadjini.telegram.converter.service.stream;

public class TelegramVoiceConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        if (conversionContext.outputFormat().canBeSentAsVoice()) {
            conversionContext.audioSamplingFrequency("48000");
        }

        super.prepare(conversionContext);
    }
}

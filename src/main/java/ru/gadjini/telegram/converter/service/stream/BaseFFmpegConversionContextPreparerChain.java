package ru.gadjini.telegram.converter.service.stream;

public class BaseFFmpegConversionContextPreparerChain implements FFmpegConversionContextPreparerChain {

    private FFmpegConversionContextPreparerChain next;

    @Override
    public final FFmpegConversionContextPreparerChain setNext(FFmpegConversionContextPreparerChain next) {
        this.next = next;

        return next;
    }

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        if (next != null) {
            next.prepare(conversionContext);
        }
    }
}

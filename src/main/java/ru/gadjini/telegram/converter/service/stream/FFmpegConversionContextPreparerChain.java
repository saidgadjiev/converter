package ru.gadjini.telegram.converter.service.stream;

public interface FFmpegConversionContextPreparerChain {

    FFmpegConversionContextPreparerChain setNext(FFmpegConversionContextPreparerChain next);

    void prepare(FFmpegConversionContext conversionContext) throws InterruptedException;
}

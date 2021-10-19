package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public interface FFmpegCommandBuilderChain {

    FFmpegCommandBuilderChain setNext(FFmpegCommandBuilderChain next);

    void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException;
}

package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class BaseFFmpegCommandBuilderChain implements FFmpegCommandBuilderChain {

    private FFmpegCommandBuilderChain next;

    @Override
    public final FFmpegCommandBuilderChain setNext(FFmpegCommandBuilderChain next) {
        this.next = next;

        return next;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        if (next != null) {
            next.prepareCommand(command, conversionContext);
        }
    }
}

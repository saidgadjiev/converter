package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public class BaseStreamProcessor implements StreamProcessor {

    private StreamProcessor next;

    @Override
    public final StreamProcessor setNext(StreamProcessor next) {
        this.next = next;

        return next;
    }

    @Override
    public void process(Format format, List<FFprobeDevice.FFProbeStream> allStreams, Object... args) {
        if (next != null) {
            next.process(format, allStreams, args);
        }
    }
}

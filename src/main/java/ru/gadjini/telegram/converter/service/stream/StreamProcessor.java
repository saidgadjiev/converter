package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public interface StreamProcessor {

    StreamProcessor setNext(StreamProcessor next);

    void process(Format format, List<FFprobeDevice.FFProbeStream> allStreams, Object... args);
}

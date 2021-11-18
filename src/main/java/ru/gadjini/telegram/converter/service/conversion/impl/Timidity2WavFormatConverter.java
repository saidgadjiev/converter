package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.timidity.TimidityDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MID;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WAV;

@Component
public class Timidity2WavFormatConverter extends BaseAudioConverter {

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(MID), List.of(WAV));
    }};

    private TimidityDevice timidityDevice;

    @Autowired
    public Timidity2WavFormatConverter(TimidityDevice timidityDevice) {
        super(MAP);
        this.timidityDevice = timidityDevice;
    }

    @Override
    public void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem fileQueueItem, Format targetFormat) throws InterruptedException {
        timidityDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), "-Ow", "-o");
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

public class FFmpegAudioCompressConverter extends BaseAudioConverter {

    public static final String AUTO_BITRATE = "auto";

    protected FFmpegAudioCompressConverter(Map<List<Format>, List<Format>> map) {
        super(map);
    }

    @Override
    protected void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem) {

    }
}

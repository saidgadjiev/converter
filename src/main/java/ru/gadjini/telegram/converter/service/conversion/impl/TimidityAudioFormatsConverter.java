package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class TimidityAudioFormatsConverter extends BaseAudioConverter {

    private static final String TAG = "timidityaudio";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(MID), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WMA, OPUS, SPX, M4A, RA));
    }};

    private Timidity2WavFormatConverter wavFormatConverter;

    private FFmpegAudioFormatsConverter fFmpegAudioFormatsConverter;

    @Autowired
    public TimidityAudioFormatsConverter(Timidity2WavFormatConverter wavFormatConverter, FFmpegAudioFormatsConverter fFmpegAudioFormatsConverter) {
        super(MAP);
        this.wavFormatConverter = wavFormatConverter;
        this.fFmpegAudioFormatsConverter = fFmpegAudioFormatsConverter;
    }

    @Override
    public void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem fileQueueItem) {
        SmartTempFile wavFile = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, WAV.getExt());
        try {
            wavFormatConverter.doConvertAudio(in, wavFile, fileQueueItem);
            fFmpegAudioFormatsConverter.doConvertAudio(wavFile, out, fileQueueItem);
        } finally {
            wavFile.smartDelete();
        }
    }
}

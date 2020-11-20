package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegAudioFormatsConverter extends BaseAudioConverter {

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(AAC), List.of(AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A));
        put(List.of(AMR), List.of(AAC, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A));
        put(List.of(AIFF), List.of(AMR, AAC, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A));
        put(List.of(FLAC), List.of(AMR, AAC, AIFF, MP3, OGG, WAV, WMA, SPX, OPUS, M4A));
        put(List.of(MP3), List.of(AMR, AAC, AIFF, FLAC, OGG, WAV, WMA, SPX, OPUS, M4A));
        put(List.of(OGG), List.of(AMR, AAC, AIFF, FLAC, MP3, WAV, WMA, SPX, OPUS, M4A));
        put(List.of(WAV), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WMA, SPX, OPUS, M4A));
        put(List.of(WMA), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WAV, SPX, OPUS, M4A));
        put(List.of(OPUS), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, M4A));
        put(List.of(SPX), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A));
        put(List.of(M4A), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS));
        put(List.of(M4B), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A));
    }};

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioFormatsConverter(FFmpegDevice fFmpegDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
    }

    @Override
    public void doConvert(SmartTempFile in, SmartTempFile out, ConversionQueueItem fileQueueItem) {
        fFmpegDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), getOptions(fileQueueItem.getTargetFormat()));
    }

    private String[] getOptions(Format target) {
        if (target == AMR) {
            return new String[]{
                    "-ar", "8000", "-ac", "1"
            };
        }
        if (target == OGG) {
            return new String[]{
                    "-c:a", "libvorbis", "-q:a", "4"
            };
        }
        return new String[0];
    }
}

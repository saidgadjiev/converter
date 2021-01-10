package ru.gadjini.telegram.converter.service.conversion.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegAudioCompressConverter extends BaseAudioConverter {

    public static final String AUTO_BITRATE = "auto";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, RA, RM, M4A, M4B), List.of(COMPRESS)
    );

    private Gson gson;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegAudioCompressConverter(Gson gson, FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.gson = gson;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    protected void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem) {
        SettingsState settingsState = gson.fromJson((JsonElement) conversionQueueItem.getExtra(), SettingsState.class);

        if (settingsState.getBitrate().equals(AUTO_BITRATE)) {
            long duration = fFprobeDevice.getDurationInSeconds(in.getAbsolutePath());
            long estimatedSize = conversionQueueItem.getSize() / 5;
            fFmpegDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), getCompressOptions(autoBitrate(estimatedSize, duration)));
        } else {
            fFmpegDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), settingsState.getBitrate());
        }
    }

    private String autoBitrate(long fileSize, long duration) {
        double bitrate = fileSize * 8192.0 / duration;

        return String.valueOf(bitrate);
    }

    private String[] getCompressOptions(String bitrate) {
        return new String[]{
                "-b:a", bitrate + "k"
        };
    }
}

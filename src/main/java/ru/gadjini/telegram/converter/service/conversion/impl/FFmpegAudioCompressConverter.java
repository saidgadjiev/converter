package ru.gadjini.telegram.converter.service.conversion.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegAudioCompressConverter extends BaseAudioConverter {

    public static final String AUTO_BITRATE = "18";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, RA, RM, M4A, M4B), List.of(COMPRESS)
    );

    private Gson gson;

    private FFmpegDevice fFmpegDevice;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public FFmpegAudioCompressConverter(Gson gson, FFmpegDevice fFmpegDevice,
                                        UserService userService, LocalisationService localisationService) {
        super(MAP);
        this.gson = gson;
        this.fFmpegDevice = fFmpegDevice;
        this.userService = userService;
        this.localisationService = localisationService;
    }

    @Override
    public Map<List<Format>, List<Format>> getConversionMap() {
        return Map.of();
    }

    @Override
    protected void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem) {
        String bitrate = AUTO_BITRATE;
        Format compressionFormat = Format.MP3;
        if (conversionQueueItem.getExtra() != null) {
            SettingsState settingsState = gson.fromJson((JsonElement) conversionQueueItem.getExtra(), SettingsState.class);
            bitrate = settingsState.getBitrate();
            compressionFormat = settingsState.getFormatOrDefault(MP3);
        }

        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
        commandBuilder.mapAudio();
        commandBuilder.ba(bitrate + "k");

        if (MP3.equals(compressionFormat)) {
            commandBuilder.ar("22050");
        }

        try {
            fFmpegDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        }
        if (in.length() <= out.length()) {
            Locale localeOrDefault = userService.getLocaleOrDefault(conversionQueueItem.getUserId());
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCOMPRESSIBLE_AUDIO, localeOrDefault))
                    .setReplyToMessageId(conversionQueueItem.getReplyToMessageId());
        }
    }
}

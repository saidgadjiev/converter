package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class AudioBassBooster extends BaseAudioConverter {

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, RA, RM, M4A, M4B), List.of(BASS_BOOST)
    );

    private Jackson json;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private FFmpegAudioStreamConversionHelper fFmpegAudioHelper;

    private LocalisationService localisationService;

    @Autowired
    public AudioBassBooster(Jackson jackson, FFmpegDevice fFmpegDevice,
                            FFprobeDevice fFprobeDevice, UserService userService,
                            FFmpegAudioStreamConversionHelper fFmpegAudioHelper,
                            LocalisationService localisationService) {
        super(MAP);
        this.json = jackson;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;
        this.fFmpegAudioHelper = fFmpegAudioHelper;
        this.localisationService = localisationService;
    }

    @Override
    protected void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem) {
        SettingsState settingsState = json.convertValue(conversionQueueItem.getExtra(), SettingsState.class);
        String bassBoost = settingsState.getBassBoost();

        if (StringUtils.isBlank(bassBoost)) {
            throw new UserException(localisationService.getMessage(
                    ConverterMessagesProperties.MESSAGE_AUDIO_CHOOSE_BASS_BOOST, userService.getLocaleOrDefault(conversionQueueItem.getUserId())
            ));
        }

        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite().input(in.getAbsolutePath());

        try {
            fFmpegAudioHelper.addCopyableCoverArtOptions(in, out, commandBuilder);
            if (conversionQueueItem.getFirstFileFormat().canBeSentAsVoice()) {
                fFmpegAudioHelper.convertAudioCodecsForTelegramVoice(commandBuilder);
            } else {
                fFmpegAudioHelper.convertAudioCodecs(commandBuilder, conversionQueueItem.getFirstFileFormat());
            }
            List<FFprobeDevice.FFProbeStream> audioStreams = fFprobeDevice.getAudioStreams(in.getAbsolutePath());
            commandBuilder.keepAudioBitRate(audioStreams.iterator().next().getBitRate());
            fFmpegAudioHelper.addAudioTargetOptions(commandBuilder, conversionQueueItem.getFirstFileFormat(), false);
            commandBuilder.af("bass=g=" + bassBoost);

            commandBuilder.out(out.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.buildFullCommand());
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        }
    }
}

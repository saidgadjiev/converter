package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class AudioBassBooster extends BaseAudioConverter {

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, RA, RM, M4A, M4B), List.of(BASS_BOOST)
    );

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private final FFmpegConversionContextPreparerChain contextPreparerChain;

    private Jackson json;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public AudioBassBooster(Jackson jackson, FFmpegDevice fFmpegDevice,
                            FFprobeDevice fFprobeDevice, UserService userService,
                            LocalisationService localisationService,
                            FFmpegCommandBuilderFactory commandBuilderFactory,
                            FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory) {
        super(MAP);
        this.json = jackson;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;
        this.localisationService = localisationService;

        this.contextPreparerChain = contextPreparerChainFactory.bassBoostPreparer();

        this.commandBuilderChain = commandBuilderFactory.quiteInput();
        this.commandBuilderChain.setNext(commandBuilderFactory.audioCover())
                .setNext(commandBuilderFactory.audioConversion())
                .setNext(commandBuilderFactory.audioBassBoost())
                .setNext(commandBuilderFactory.telegramVoiceConversion())
                .setNext(commandBuilderFactory.output());
    }

    @Override
    protected void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem, Format targetFormat) throws InterruptedException {
        SettingsState settingsState = json.convertValue(conversionQueueItem.getExtra(), SettingsState.class);
        String bassBoost = settingsState.getBassBoost();

        if (StringUtils.isBlank(bassBoost)) {
            throw new UserException(localisationService.getMessage(
                    ConverterMessagesProperties.MESSAGE_AUDIO_CHOOSE_BASS_BOOST, userService.getLocaleOrDefault(conversionQueueItem.getUserId())
            ));
        }

        List<FFprobeDevice.FFProbeStream> audioStreams = fFprobeDevice.getAudioStreams(in.getAbsolutePath(), FormatCategory.AUDIO);
        FFmpegConversionContext conversionContext = FFmpegConversionContext.from(in, out, targetFormat, audioStreams)
                .putExtra(FFmpegConversionContext.BASS_BOOST, bassBoost);

        contextPreparerChain.prepare(conversionContext);

        FFmpegCommand command = new FFmpegCommand();
        commandBuilderChain.prepareCommand(command, conversionContext);

        fFmpegDevice.execute(command.toCmd());
    }
}

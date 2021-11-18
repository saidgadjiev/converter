package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegAudioCompressConverter extends BaseAudioConverter {

    public static final Format DEFAULT_AUDIO_COMPRESS_FORMAT = MP3;

    public static final String MP3_FREQUENCY_44 = "44";

    public static final String MP3_FREQUENCY_22 = "22";

    public static final String AUTO_BITRATE = "32";

    private static final Map<Format, String> DEFAULT_FREQUENCIES = new HashMap<>() {{
        put(OPUS, null);
        put(MP3, MP3_FREQUENCY_44);
    }};

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, RA, RM, M4A, M4B), List.of(COMPRESS)
    );

    private final FFmpegCommandBuilderChain commandBuilder;

    private final FFmpegConversionContextPreparerChain conversionContextPreparerChain;

    private Jackson json;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public FFmpegAudioCompressConverter(Jackson jackson, FFmpegDevice fFmpegDevice,
                                        UserService userService,
                                        LocalisationService localisationService,
                                        FFmpegCommandBuilderFactory commandBuilderFactory,
                                        FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory,
                                        FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.json = jackson;
        this.fFmpegDevice = fFmpegDevice;
        this.userService = userService;
        this.localisationService = localisationService;

        this.commandBuilder = commandBuilderFactory.quite();
        this.fFprobeDevice = fFprobeDevice;
        commandBuilder.setNext(commandBuilderFactory.input())
                .setNext(commandBuilderFactory.audioConversion())
                .setNext(commandBuilderFactory.audioCompression())
                .setNext(commandBuilderFactory.output());

        this.conversionContextPreparerChain = contextPreparerChainFactory.telegramVoiceContextPreparer();
    }

    public static String getDefaultFrequency(Format format) {
        return DEFAULT_FREQUENCIES.get(format);
    }

    @Override
    protected void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem, Format targetFormat) {
        String bitrate = AUTO_BITRATE;
        Format compressionFormat = DEFAULT_AUDIO_COMPRESS_FORMAT;
        String frequency = MP3_FREQUENCY_44;
        if (conversionQueueItem.getExtra() != null) {
            SettingsState settingsState = json.convertValue(conversionQueueItem.getExtra(), SettingsState.class);
            bitrate = settingsState.getBitrate();
            compressionFormat = settingsState.getFormatOrDefault(MP3);
            frequency = settingsState.getFrequencyOrDefault(getDefaultFrequency(compressionFormat));
        }

        try {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(in.getAbsolutePath());
            FFmpegConversionContext conversionContext = new FFmpegConversionContext()
                    .streams(allStreams)
                    .input(in)
                    .output(out)
                    .outputFormat(compressionFormat)
                    .putExtra(FFmpegConversionContext.AUDIO_BITRATE, Integer.parseInt(bitrate))
                    .putExtra(FFmpegConversionContext.FREQUENCY, frequency);
            conversionContextPreparerChain.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilder.prepareCommand(command, conversionContext);

            fFmpegDevice.execute(command.toCmd());
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

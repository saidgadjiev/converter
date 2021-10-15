package ru.gadjini.telegram.converter.service.conversion.impl;

import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ru.gadjini.telegram.converter.service.conversion.impl.VideoCutter.PERIOD_FORMATTER;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.CUT;

@Component
public class AudioCutter extends BaseAudioConverter {

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.AUDIO), List.of(CUT)
    );

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioStreamConversionHelper audioHelper;

    private UserService userService;

    private LocalisationService localisationService;

    private Jackson jackson;

    @Autowired
    public AudioCutter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                       FFmpegAudioStreamConversionHelper audioHelper, UserService userService,
                       LocalisationService localisationService, Jackson jackson) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.audioHelper = audioHelper;
        this.userService = userService;
        this.localisationService = localisationService;
        this.jackson = jackson;
    }

    @Override
    protected void doConvertAudio(SmartTempFile file, SmartTempFile result, ConversionQueueItem fileQueueItem) throws InterruptedException {
        long durationInSeconds = fFprobeDevice.getDurationInSeconds(file.getAbsolutePath());

        SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
        validateRange(fileQueueItem.getReplyToMessageId(), settingsState.getCutStartPoint(), settingsState.getCutEndPoint(),
                durationInSeconds, userService.getLocaleOrDefault(fileQueueItem.getUserId()));
        String startPoint = PERIOD_FORMATTER.print(settingsState.getCutStartPoint());
        String duration = String.valueOf(settingsState.getCutEndPoint().minus(settingsState.getCutStartPoint()).toStandardDuration().getStandardSeconds());

        FFmpegCommand commandBuilder = new FFmpegCommand();

        commandBuilder.hideBanner().quite().ss(startPoint).input(file.getAbsolutePath()).t(duration);
        audioHelper.addCopyableCoverArtOptions(file, result, commandBuilder);
        if (fileQueueItem.getFirstFileFormat().canBeSentAsVoice()) {
            audioHelper.convertAudioCodecsForTelegramVoice(commandBuilder);
        } else {
            audioHelper.convertAudioCodecs(commandBuilder, fileQueueItem.getFirstFileFormat());
        }
        audioHelper.addAudioTargetOptions(commandBuilder, fileQueueItem.getFirstFileFormat());
        commandBuilder.out(result.getAbsolutePath());

        fFmpegDevice.execute(commandBuilder.buildFullCommand());
    }

    private void validateRange(Integer replyMessageId, Period start, Period end, Long totalLength, Locale locale) {
        if (totalLength == null) {
            return;
        }
        long startSeconds = start.toStandardDuration().getStandardSeconds();
        long endSeconds = end.toStandardDuration().getStandardSeconds();

        if (startSeconds < 0 || startSeconds > totalLength
                || endSeconds < 0 || endSeconds > totalLength) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CUT_OUT_OF_RANGE,
                    new Object[]{
                            PERIOD_FORMATTER.print(new Period(totalLength * 1000L)),
                            PERIOD_FORMATTER.print(start),
                            PERIOD_FORMATTER.print(end)
                    }, locale)).setReplyToMessageId(replyMessageId);
        }
    }
}

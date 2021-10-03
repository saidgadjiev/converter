package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.PhotoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("CPD-START")
public class VideoScreenshotTaker extends BaseAny2AnyConverter {

    public static final String TAG = "vscreenshot";

    public static final Period AT_START = Period.ZERO;

    private static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .printZeroIfSupported()
            .minimumPrintedDigits(2)
            .rejectSignedValues(true)
            .appendHours()
            .appendSeparator(":")
            .appendMinutes()
            .appendSeparator(":")
            .appendSeconds()
            .toFormatter();

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.SCREENSHOT)
    );

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private LocalisationService localisationService;

    private Jackson jackson;

    @Autowired
    public VideoScreenshotTaker(FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                UserService userService, LocalisationService localisationService, Jackson jackson) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;
        this.localisationService = localisationService;
        this.jackson = jackson;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, Format.JPG.getExt());
        try {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams));

            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            Period sp = getStartPoint(srcWhd, settingsState);
            String startPoint = PERIOD_FORMATTER.print(sp.normalizedStandard());

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            commandBuilder.hideBanner().quite().ss(startPoint).input(file.getAbsolutePath())
                    .mapVideo(fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams)).vframes("1").qv("2")
                    .out(result.getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());
            String caption = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VSCREENSHOT_RESULT,
                    new Object[]{startPoint}, userService.getLocaleOrDefault(fileQueueItem.getUserId()));

            return new PhotoResult(fileName, result, caption);
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private Period getStartPoint(FFprobeDevice.WHD whd, SettingsState settingsState) {
        if (settingsState.getCutStartPoint() != null) {
            return settingsState.getCutStartPoint();
        }
        if (whd.getDuration() == null) {
            return Period.seconds(0);
        }

        if (whd.getDuration() < 60) {
            return Period.seconds(whd.getDuration().intValue());
        }

        return Period.seconds(new RandomDataGenerator().nextInt(30, whd.getDuration().intValue() - 30));
    }
}

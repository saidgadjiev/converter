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
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.PhotoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
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
import java.util.Locale;
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

    private final FFmpegCommandBuilderChain commandBuilder;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private LocalisationService localisationService;

    private Jackson jackson;

    @Autowired
    public VideoScreenshotTaker(FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                UserService userService, LocalisationService localisationService, Jackson jackson,
                                FFmpegCommandBuilderFactory commandBuilderFactory) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;
        this.localisationService = localisationService;
        this.jackson = jackson;

        this.commandBuilder = commandBuilderFactory.quite();
        commandBuilder.setNext(commandBuilderFactory.cutStartPoint())
                .setNext(commandBuilderFactory.input())
                .setNext(commandBuilderFactory.videoScreenshot())
                .setNext(commandBuilderFactory.output());
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, Format.JPG.getExt());
        try {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreamsWithoutBitrate(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams));

            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            if (settingsState.getCutStartPoint() != null) {
                validateRange(fileQueueItem.getReplyToMessageId(), settingsState.getCutStartPoint(),
                        srcWhd.getDuration(), locale);
            }
            Period sp = getStartPoint(srcWhd, settingsState);
            String startPoint = PERIOD_FORMATTER.print(sp.normalizedStandard());

            FFmpegConversionContext conversionContext = FFmpegConversionContext.from(file, result, Format.JPG, allStreams)
                    .putExtra(FFmpegConversionContext.CUT_START_POINT, startPoint);
            takeScreenshot(conversionContext);
            if (result.length() == 0 && sp.toStandardSeconds().getSeconds() > 0) {
                sp = sp.minusSeconds(1);
                startPoint = PERIOD_FORMATTER.print(sp.normalizedStandard());
                conversionContext.putExtra(FFmpegConversionContext.CUT_START_POINT, startPoint);
                takeScreenshot(conversionContext);
            }

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());
            String caption = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VSCREENSHOT_RESULT,
                    new Object[]{startPoint}, locale);

            return new PhotoResult(fileName, result, caption);
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private void takeScreenshot(FFmpegConversionContext conversionContext) throws InterruptedException {
        FFmpegCommand command = new FFmpegCommand();
        commandBuilder.prepareCommand(command, conversionContext);

        fFmpegDevice.execute(command.toCmd());
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

    private void validateRange(Integer replyMessageId, Period start, Long totalLength, Locale locale) {
        if (totalLength == null) {
            return;
        }
        long startSeconds = start.toStandardDuration().getStandardSeconds();

        if (startSeconds < 0 || startSeconds > totalLength) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_SCREENSHOT_OUT_OF_RANGE,
                    new Object[]{
                            PERIOD_FORMATTER.print(new Period(totalLength * 1000L).normalizedStandard()),
                            PERIOD_FORMATTER.print(start.normalizedStandard())
                    }, locale)).setReplyToMessageId(replyMessageId);
        }
    }
}

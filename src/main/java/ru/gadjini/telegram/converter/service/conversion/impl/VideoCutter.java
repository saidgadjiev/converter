package ru.gadjini.telegram.converter.service.conversion.impl;

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
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
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
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.CUT;

@Component
@SuppressWarnings("CPD-START")
public class VideoCutter extends BaseAny2AnyConverter {

    public static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .printZeroIfSupported()
            .minimumPrintedDigits(2)
            .rejectSignedValues(true)
            .appendHours()
            .appendSeparator(":")
            .appendMinutes()
            .appendSeparator(":")
            .appendSeconds()
            .toFormatter();

    public static final String AT_START = "atStart";

    public static final String AT_END = "atEnd";

    private static final String TAG = "vcut";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(CUT)
    );

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private final FFmpegConversionContextPreparerChain contextPreparerChain;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private LocalisationService localisationService;

    private Jackson jackson;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public VideoCutter(FFmpegVideoStreamConversionHelper fFmpegVideoHelper, FFmpegDevice fFmpegDevice,
                       FFprobeDevice fFprobeDevice, UserService userService, LocalisationService localisationService,
                       Jackson jackson, FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                       FFmpegCommandBuilderFactory commandBuilderFactory,
                       FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory,
                       VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoResultBuilder = videoResultBuilder;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;
        this.localisationService = localisationService;
        this.jackson = jackson;
        this.callbackHandlerFactory = callbackHandlerFactory;

        this.commandBuilderChain = commandBuilderFactory.quite();
        commandBuilderChain.setNext(commandBuilderFactory.cutStartPoint())
                .setNext(commandBuilderFactory.input())
                .setNext(commandBuilderFactory.streamDuration())
                .setNext(commandBuilderFactory.videoConversion())
                .setNext(commandBuilderFactory.audioConversion())
                .setNext(commandBuilderFactory.subtitlesConversion())
                .setNext(commandBuilderFactory.webmQuality())
                .setNext(commandBuilderFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderFactory.output());

        this.contextPreparerChain = contextPreparerChainFactory.telegramVideoContextPreparer();
        contextPreparerChain.setNext(contextPreparerChainFactory.subtitlesContextPreparer());
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams));

            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            validateRange(fileQueueItem.getReplyToMessageId(), settingsState.getCutStartPoint(), settingsState.getCutEndPoint(), srcWhd.getDuration(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            doCut(file, result, settingsState.getCutStartPoint(), settingsState.getCutEndPoint(),
                    srcWhd.getDuration(), fileQueueItem, true);

            return videoResultBuilder.build(fileQueueItem, fileQueueItem.getFirstFileFormat(), result);
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    void doCut(SmartTempFile file, SmartTempFile result,
               Period sp, Period ep, Long knownDuration,
               ConversionQueueItem fileQueueItem, boolean withProgress) throws InterruptedException {
        String startPoint = PERIOD_FORMATTER.print(sp.normalizedStandard());
        if (knownDuration != null && sp.toStandardSeconds().getSeconds() > knownDuration) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INVALID_START_POINT,
                    new Object[]{
                            startPoint, PERIOD_FORMATTER.print(Period.seconds(knownDuration.intValue()).normalizedStandard())
                    },
                    userService.getLocaleOrDefault(fileQueueItem.getUserId())));
        }

        List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());

        if (knownDuration != null && ep.toStandardSeconds().getSeconds() > knownDuration.intValue()) {
            ep = Period.seconds(knownDuration.intValue());
        }

        long duration = ep.minus(sp).toStandardDuration().getStandardSeconds();

        FFmpegConversionContext conversionContext = new FFmpegConversionContext();
        conversionContext.streams(allStreams)
                .input(file)
                .output(result)
                .outputFormat(fileQueueItem.getFirstFileFormat())
                .putExtra(FFmpegConversionContext.CUT_START_POINT, startPoint)
                .putExtra(FFmpegConversionContext.STREAM_DURATION, duration);
        contextPreparerChain.prepare(conversionContext);

        FFmpegCommand command = new FFmpegCommand();
        commandBuilderChain.prepareCommand(command, conversionContext);

        FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = withProgress
                ? callbackHandlerFactory.createCallback(fileQueueItem, duration, userService.getLocaleOrDefault(fileQueueItem.getUserId()))
                : null;
        fFmpegDevice.execute(command.toCmd(), callback);
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
                            PERIOD_FORMATTER.print(new Period(totalLength * 1000L).normalizedStandard()),
                            PERIOD_FORMATTER.print(start.normalizedStandard()),
                            PERIOD_FORMATTER.print(end.normalizedStandard())
                    }, locale)).setReplyToMessageId(replyMessageId);
        }
    }
}

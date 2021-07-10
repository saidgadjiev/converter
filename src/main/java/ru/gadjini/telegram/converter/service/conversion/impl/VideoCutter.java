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
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
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
import java.util.Locale;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.CUT;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

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

    private static final String TAG = "vcut";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(CUT)
    );

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private LocalisationService localisationService;

    private Jackson jackson;

    @Autowired
    public VideoCutter(FFmpegVideoStreamConversionHelper fFmpegVideoHelper, FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                       FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper, FFmpegDevice fFmpegDevice,
                       FFprobeDevice fFprobeDevice, UserService userService, LocalisationService localisationService, Jackson jackson) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.fFmpegSubtitlesHelper = fFmpegSubtitlesHelper;
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
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), FFmpegVideoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));

            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            validateRange(fileQueueItem.getReplyToMessageId(), settingsState.getCutStartPoint(), settingsState.getCutEndPoint(), srcWhd.getDuration(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            String startPoint = PERIOD_FORMATTER.print(settingsState.getCutStartPoint());
            String duration = String.valueOf(settingsState.getCutEndPoint().minus(settingsState.getCutStartPoint()).toStandardDuration().getStandardSeconds());

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            commandBuilder.hideBanner().quite().ss(startPoint).input(file.getAbsolutePath()).t(duration);
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                fFmpegVideoHelper.convertVideoCodecsForTelegramVideo(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat());
            } else {
                fFmpegVideoHelper.convertVideoCodecs(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat(), result);
            }
            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());
            FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                videoAudioConversionHelper.convertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
            } else {
                videoAudioConversionHelper.convertAudioCodecs(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat());
            }
            fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, allStreams, result, fileQueueItem.getFirstFileFormat());
            if (WEBM.equals(fileQueueItem.getFirstFileFormat())) {
                commandBuilder.vp8QualityOptions();
            }
            commandBuilder.fastConversion();

            commandBuilder.defaultOptions().out(result.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD wdh = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), wdh.getWidth(), wdh.getHeight(),
                        wdh.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming());
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem));
            }
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
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

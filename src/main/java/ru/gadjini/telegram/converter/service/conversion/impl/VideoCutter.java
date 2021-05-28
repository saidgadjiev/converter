package ru.gadjini.telegram.converter.service.conversion.impl;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.CUT;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

@Component
@SuppressWarnings("CPD-START")
public class VideoCutter extends BaseAny2AnyConverter {

    public static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .printZeroIfSupported()
            .minimumPrintedDigits(2)
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

    private FFmpegVideoHelper fFmpegVideoHelper;

    private FFmpegAudioHelper fFmpegAudioHelper;

    private FFmpegSubtitlesHelper fFmpegSubtitlesHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private Jackson jackson;

    @Autowired
    public VideoCutter(FFmpegVideoHelper fFmpegVideoHelper, FFmpegAudioHelper fFmpegAudioHelper,
                       FFmpegSubtitlesHelper fFmpegSubtitlesHelper, FFmpegDevice fFmpegDevice,
                       FFprobeDevice fFprobeDevice, Jackson jackson) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegAudioHelper = fFmpegAudioHelper;
        this.fFmpegSubtitlesHelper = fFmpegSubtitlesHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.jackson = jackson;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        fileQueueItem.setTargetFormat(fileQueueItem.getFirstFileFormat());
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            String startPoint = PERIOD_FORMATTER.print(settingsState.getCutStartPoint());
            String duration = String.valueOf(settingsState.getCutEndPoint().minus(settingsState.getCutStartPoint()).toStandardDuration().getStandardSeconds());

            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            commandBuilder.hideBanner().quite().ss(startPoint).input(file.getAbsolutePath()).t(duration);
            if (fileQueueItem.getTargetFormat().canBeSentAsVideo()) {
                fFmpegVideoHelper.convertVideoCodecsForTelegramVideo(commandBuilder, allStreams, fileQueueItem.getTargetFormat());
            } else {
                fFmpegVideoHelper.convertVideoCodecs(commandBuilder, allStreams, fileQueueItem.getTargetFormat(), result);
            }
            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getTargetFormat());
            if (WEBM.equals(fileQueueItem.getTargetFormat())) {
                commandBuilder.crf("10");
            }
            fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(commandBuilder, allStreams, result, fileQueueItem.getTargetFormat());
            if (fileQueueItem.getTargetFormat().canBeSentAsVideo()) {
                fFmpegAudioHelper.convertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
            } else {
                fFmpegAudioHelper.convertAudioCodecs(commandBuilder, allStreams, fileQueueItem.getTargetFormat());
            }
            commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
            commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);

            commandBuilder.defaultOptions().out(result.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

            if (fileQueueItem.getTargetFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

                return new VideoResult(fileName, result, fileQueueItem.getTargetFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getTargetFormat().supportsStreaming());
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem));
            }
        } catch (CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}

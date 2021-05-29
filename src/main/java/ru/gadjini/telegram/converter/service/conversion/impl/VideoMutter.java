package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MUTE;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

@Component
@SuppressWarnings("PMD")
public class VideoMutter extends BaseAny2AnyConverter {

    private static final String TAG = "vmute";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(MUTE)
    );

    private FFmpegVideoHelper fFmpegVideoHelper;

    private FFmpegSubtitlesHelper fFmpegSubtitlesHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public VideoMutter(FFmpegVideoHelper fFmpegVideoHelper, FFmpegSubtitlesHelper fFmpegSubtitlesHelper,
                       FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegSubtitlesHelper = fFmpegSubtitlesHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            commandBuilder.hideBanner().quite().input(file.getAbsolutePath());
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                fFmpegVideoHelper.copyOrConvertVideoCodecsForTelegramVideo(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat());
            } else {
                fFmpegVideoHelper.copyOrConvertVideoCodecs(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat(), result);
            }
            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());
            if (WEBM.equals(fileQueueItem.getFirstFileFormat())) {
                commandBuilder.crf("10");
            }
            fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(commandBuilder, allStreams, result, fileQueueItem.getFirstFileFormat());
            commandBuilder.an()
                    .preset(FFmpegCommandBuilder.PRESET_VERY_FAST)
                    .deadline(FFmpegCommandBuilder.DEADLINE_REALTIME)
                    .defaultOptions().out(result.getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming());
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

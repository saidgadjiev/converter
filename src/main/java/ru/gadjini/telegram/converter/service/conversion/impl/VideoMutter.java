package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
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

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private CaptionGenerator captionGenerator;

    @Autowired
    public VideoMutter(FFmpegVideoStreamConversionHelper fFmpegVideoHelper, FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper,
                       FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice, CaptionGenerator captionGenerator) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegSubtitlesHelper = fFmpegSubtitlesHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.captionGenerator = captionGenerator;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            fFmpegVideoHelper.validateVideoIntegrity(file, result);
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            commandBuilder.hideBanner().quite().input(file.getAbsolutePath());
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                fFmpegVideoHelper.copyOrConvertVideoCodecsForTelegramVideo(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat(), fileQueueItem.getSize());
            } else {
                fFmpegVideoHelper.copyOrConvertVideoCodecs(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat(), result, fileQueueItem.getSize());
            }
            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());
            FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
            fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, allStreams,
                    result, fileQueueItem.getFirstFileFormat());
            if (WEBM.equals(fileQueueItem.getFirstFileFormat())) {
                commandBuilder.vp8QualityOptions();
            }
            commandBuilder.an()
                    .fastConversion()
                    .defaultOptions().out(result.getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            String caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource());
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), caption);
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem), caption);
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

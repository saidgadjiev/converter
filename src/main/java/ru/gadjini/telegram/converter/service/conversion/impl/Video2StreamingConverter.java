package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Component
public class Video2StreamingConverter extends BaseAny2AnyConverter {

    private static final String TAG = "v2streaming";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.STREAM)
    );

    private FFprobeDevice fFprobeDevice;

    private FFmpegVideoConverter fFmpegVideoFormatsConverter;

    private FFmpegVideoHelper fFmpegVideoHelper;

    private FFmpegAudioHelper fFmpegAudioHelper;

    @Autowired
    public Video2StreamingConverter(FFprobeDevice fFprobeDevice, FFmpegVideoConverter fFmpegVideoFormatsConverter,
                                    FFmpegVideoHelper fFmpegVideoHelper,
                                    FFmpegAudioHelper fFmpegAudioHelper) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegVideoFormatsConverter = fFmpegVideoFormatsConverter;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegAudioHelper = fFmpegAudioHelper;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        fileQueueItem.setTargetFormat(fileQueueItem.getTargetFormat().getAssociatedFormat());

        if (fileQueueItem.getFirstFileFormat().supportsStreaming()) {
            ConversionResult conversionResult = doConvertStreamingVideo(fileQueueItem);
            if (conversionResult == null) {
                return doConvertNonStreamingVideo(fileQueueItem);
            } else {
                return conversionResult;
            }
        } else {
            return doConvertNonStreamingVideo(fileQueueItem);
        }
    }

    private ConversionResult doConvertStreamingVideo(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        SmartTempFile result = tempFileService().getTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(),
                TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            if (fFmpegVideoHelper.isVideoStreamsValidForTelegramVideo(allStreams)
                    && fFmpegAudioHelper.isAudioStreamsValidForTelegramVideo(allStreams)) {
                Files.move(file.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING);

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getTargetFormat().supportsStreaming());
            } else {
                return null;
            }
        } catch (CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private ConversionResult doConvertNonStreamingVideo(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(),
                TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            return fFmpegVideoFormatsConverter.doConvert(file, result, fileQueueItem);
        } catch (CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}


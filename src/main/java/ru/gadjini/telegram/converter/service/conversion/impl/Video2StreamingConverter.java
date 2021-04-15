package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

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

    @Autowired
    public Video2StreamingConverter(FFprobeDevice fFprobeDevice, FFmpegVideoConverter fFmpegVideoFormatsConverter) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegVideoFormatsConverter = fFmpegVideoFormatsConverter;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        fileQueueItem.setTargetFormat(fileQueueItem.getTargetFormat().getAssociatedFormat());

        if (fileQueueItem.getFirstFileFormat().supportsStreaming()) {
            return doConvertStreamingVideo(fileQueueItem);
        } else {
            return doConvertNonStreamingVideo(fileQueueItem);
        }
    }

    private ConversionResult doConvertStreamingVideo(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(),
                TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            FileUtils.copyFile(file.getFile(), result.getFile());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                    whd.getDuration(), true);
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
            return fFmpegVideoFormatsConverter.doConvert(file, result, fileQueueItem);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}


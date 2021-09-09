package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
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

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private CaptionGenerator captionGenerator;

    @Autowired
    public Video2StreamingConverter(FFprobeDevice fFprobeDevice, FFmpegVideoConverter fFmpegVideoFormatsConverter,
                                    FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                    FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                                    CaptionGenerator captionGenerator) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegVideoFormatsConverter = fFmpegVideoFormatsConverter;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.captionGenerator = captionGenerator;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
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
                TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            if (fFmpegVideoHelper.isVideoStreamsValidForTelegramVideo(allStreams)
                    && videoAudioConversionHelper.isAudioStreamsValidForTelegramVideo(allStreams)) {
                Files.move(file.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING);

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
                String generate = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource());
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), generate);
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
                TAG, Format.STREAM.getAssociatedFormat().getExt());

        try {
            return fFmpegVideoFormatsConverter.doConvert(file, result, fileQueueItem, Format.STREAM.getAssociatedFormat());
        } catch (CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}


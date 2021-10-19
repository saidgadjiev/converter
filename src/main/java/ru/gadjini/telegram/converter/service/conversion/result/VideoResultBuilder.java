package ru.gadjini.telegram.converter.service.conversion.result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

@Component
public class VideoResultBuilder {

    private CaptionGenerator captionGenerator;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public VideoResultBuilder(CaptionGenerator captionGenerator, FFmpegVideoStreamConversionHelper videoStreamConversionHelper) {
        this.captionGenerator = captionGenerator;
        this.videoStreamConversionHelper = videoStreamConversionHelper;
    }

    public ConversionResult build(ConversionQueueItem fileQueueItem, SmartTempFile result) throws InterruptedException {
        return build(fileQueueItem, fileQueueItem.getTargetFormat(), result);
    }

    public ConversionResult build(ConversionQueueItem fileQueueItem, Format targetFormat,
                                  SmartTempFile result) throws InterruptedException {
        return build(fileQueueItem, targetFormat, null, result);
    }

    public ConversionResult build(ConversionQueueItem fileQueueItem, Format targetFormat,
                                  String caption, SmartTempFile result) throws InterruptedException {
        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), targetFormat.getExt());

        caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource(), caption);
        if (targetFormat.canBeSentAsVideo()) {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(result.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));

            return new VideoResult(fileName, result, targetFormat, BaseAny2AnyConverter.downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                    whd.getDuration(), targetFormat.supportsStreaming(), caption);
        } else {
            return new FileResult(fileName, result, BaseAny2AnyConverter.downloadThumb(fileQueueItem), caption);
        }
    }
}

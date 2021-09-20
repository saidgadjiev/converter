package ru.gadjini.telegram.converter.service.conversion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("CPD-START")
public class VideoNote2Mp4Converter extends BaseAny2AnyConverter {

    private static final String TAG = "mp42mp4";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.MP4), List.of(Format.MP4)
    );

    private FFprobeDevice fFprobeDevice;

    private CaptionGenerator captionGenerator;

    @Autowired
    public VideoNote2Mp4Converter(FFprobeDevice fFprobeDevice, CaptionGenerator captionGenerator) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.captionGenerator = captionGenerator;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        SmartTempFile result = tempFileService().getTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(),
                TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Files.move(file.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING);

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            String generate = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource());
            return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem),
                    whd.getWidth(), whd.getHeight(),
                    whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), generate);
        } catch (CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}

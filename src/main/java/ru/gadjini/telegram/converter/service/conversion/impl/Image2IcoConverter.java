package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.ImageConvertDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Image2IcoConverter extends BaseAny2AnyConverter {

    public static final String TAG = "image2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PHOTO, Format.HEIC, Format.HEIF, Format.PNG, Format.SVG,
            Format.JP2, Format.JPG, Format.BMP, Format.WEBP), List.of(Format.ICO));

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    @Autowired
    public Image2IcoConverter(TempFileService fileService, ImageConvertDevice imageDevice) {
        super(MAP);
        this.fileService = fileService;
        this.imageDevice = imageDevice;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return doConvertToIco(fileQueueItem);
    }

    private FileResult doConvertToIco(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

            imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath(),
                    "-resize", "x32", "-gravity", "center", "-crop", "32x32+0+0", "-flatten", "-colors", "256");

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

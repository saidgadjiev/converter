package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.StickerResult;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.converter.service.image.device.ImageConvertDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter {

    private static final String TAG = "image2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(PNG, PHOTO), List.of(JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(JPG), List.of(PNG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(BMP), List.of(PNG, JPG, JP2, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(WEBP), List.of(PNG, JPG, JP2, BMP, TIFF, HEIC, HEIF, STICKER),
            List.of(SVG), List.of(PNG, JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(HEIC, HEIF), List.of(JPG, JP2, BMP, WEBP, TIFF, STICKER),
            List.of(ICO), List.of(PNG, JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(JP2), List.of(PNG, JPG, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(TIFF), List.of(PDF)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    private ConversionFormatService formatService;

    @Autowired
    public Image2AnyConverter(FileManager fileManager, TempFileService fileService,
                              ImageConvertDevice imageDevice, ConversionFormatService formatService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
        this.formatService = formatService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return doConvert(fileQueueItem);
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat() != PHOTO ? fileQueueItem.getFirstFileFormat().getExt() : "tmp");

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);
            normalize(file.getFile(), fileQueueItem);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

            imageDevice.convert2Image(file.getAbsolutePath(), tempFile.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return fileQueueItem.getTargetFormat() == STICKER
                    ? new StickerResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS))
                    : new FileResult(fileName, tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (ProcessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private void normalize(File file, ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFirstFileFormat() == PHOTO) {
            Format format = formatService.getImageFormat(file, fileQueueItem.getFirstFileId());
            format = format == null ? JPG : format;
            fileQueueItem.getFirstFile().setFormat(format);
        }
    }
}

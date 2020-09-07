package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.conversion.api.Format;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.StickerResult;
import ru.gadjini.telegram.converter.service.image.device.ImageConvertDevice;
import ru.gadjini.telegram.converter.service.image.trace.ImageTracer;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    public static final String TAG = "image2";

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.PHOTO, Format.HEIC, Format.HEIF, Format.PNG, Format.SVG,
            Format.JP2, Format.JPG, Format.BMP, Format.WEBP);

    private FileManager fileManager;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    private ImageTracer imageTracer;

    @Autowired
    public Image2AnyConverter(FileManager fileManager, TempFileService fileService,
                              ConversionFormatService formatService, ImageConvertDevice imageDevice, ImageTracer imageTracer) {
        super(ACCEPT_FORMATS, formatService);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
        this.imageTracer = imageTracer;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.DOC || fileQueueItem.getTargetFormat() == Format.DOCX) {
            return doConvertToWord(fileQueueItem);
        }
        if (fileQueueItem.getFormat() == Format.HEIC && fileQueueItem.getTargetFormat() == Format.PDF) {
            return doConvertHeicToPdf(fileQueueItem);
        }
        if (fileQueueItem.getTargetFormat() == Format.ICO) {
            return doConvertToIco(fileQueueItem);
        }
        if (fileQueueItem.getTargetFormat() == Format.SVG) {
            return doConvertToSvg(fileQueueItem);
        }

        return doConvert(fileQueueItem);
    }

    private FileResult doConvertToWord(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat() != Format.PHOTO ? fileQueueItem.getFormat().getExt() : "tmp");

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            normalize(file.getFile(), fileQueueItem);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document();
            try {
                DocumentBuilder documentBuilder = new DocumentBuilder(document);
                documentBuilder.insertImage(file.getAbsolutePath());
                SmartTempFile out = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                documentBuilder.getDocument().save(out.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, out, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.cleanup();
            }
        } catch (Exception e) {
            throw new ConvertException(e);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat() != Format.PHOTO ? fileQueueItem.getFormat().getExt() : "tmp");

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            normalize(file.getFile(), fileQueueItem);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

            imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
            return fileQueueItem.getTargetFormat() == Format.STICKER
                    ? new StickerResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS))
                    : new FileResult(fileName, tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvertHeicToPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PNG.getExt());
            try {
                imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath());
                SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PDF.getExt());
                imageDevice.convert(tempFile.getAbsolutePath(), result.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                tempFile.smartDelete();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private void normalize(File file, ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFormat() == Format.PHOTO) {
            Format format = formatService.getImageFormat(file, fileQueueItem.getFileId());
            format = format == null ? Format.JPG : format;
            fileQueueItem.setFormat(format);
        }
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

    private FileResult doConvertToSvg(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.SVG.getExt());
            if (fileQueueItem.getTargetFormat() != Format.PNG) {
                SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PNG.getExt());
                try {
                    imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath());
                    imageTracer.trace(tempFile.getAbsolutePath(), result.getAbsolutePath());

                    stopWatch.stop();
                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PNG.getExt());
                    return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
                } finally {
                    tempFile.smartDelete();
                }
            } else {
                imageTracer.trace(file.getAbsolutePath(), result.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.SVG.getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

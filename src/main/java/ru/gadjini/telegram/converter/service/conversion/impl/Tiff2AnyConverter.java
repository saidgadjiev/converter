package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.tiff.TiffFrame;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.words.DocumentBuilder;
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
import ru.gadjini.telegram.smart.bot.commons.service.conversion.api.Format;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Tiff2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    public static final String TAG = "tiff2";

    private FileManager fileManager;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    @Autowired
    public Tiff2AnyConverter(ConversionFormatService formatService, FileManager fileManager,
                             TempFileService fileService, ImageConvertDevice imageDevice) {
        super(Set.of(Format.TIFF), formatService);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        return toWord(fileQueueItem);
    }

    private FileResult toWord(ConversionQueueItem fileQueueItem) {
        SmartTempFile tiff = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), tiff);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try (TiffImage image = (TiffImage) Image.load(tiff.getAbsolutePath())) {
                DocumentBuilder documentBuilder = new DocumentBuilder();
                try {
                    for (TiffFrame tiffFrame : image.getFrames()) {
                        documentBuilder.insertImage(tiffFrame.toBitmap());
                    }
                    SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                    documentBuilder.getDocument().save(result.getAbsolutePath());

                    stopWatch.stop();
                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
                } finally {
                    documentBuilder.getDocument().cleanup();
                }
            } catch (Exception ex) {
                throw new ConvertException(ex);
            }
        } finally {
            tiff.smartDelete();
        }
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile tiff = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());
       try {
           fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), tiff);

           StopWatch stopWatch = new StopWatch();
           stopWatch.start();
           SmartTempFile pdf = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PDF.getExt());
            imageDevice.convert(tiff.getAbsolutePath(), pdf.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
            return new FileResult(fileName, pdf, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            tiff.smartDelete();
        }
    }
}

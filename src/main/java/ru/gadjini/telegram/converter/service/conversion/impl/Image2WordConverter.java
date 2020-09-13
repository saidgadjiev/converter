package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Image2WordConverter extends BaseAny2AnyConverter {

    public static final String TAG = "image2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.PHOTO, Format.HEIC, Format.HEIF, Format.PNG, Format.SVG, Format.JP2, Format.JPG, Format.BMP, Format.WEBP),
            List.of(Format.DOC, Format.DOCX)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    private ConversionFormatService formatService;

    @Autowired
    public Image2WordConverter(FileManager fileManager, TempFileService fileService, ConversionFormatService formatService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.formatService = formatService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return doConvertToWord(fileQueueItem);
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

    private void normalize(File file, ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFormat() == Format.PHOTO) {
            Format format = formatService.getImageFormat(file, fileQueueItem.getFileId());
            format = format == null ? Format.JPG : format;
            fileQueueItem.setFormat(format);
        }
    }
}

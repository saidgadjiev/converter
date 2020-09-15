package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.devices.TiffDevice;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Word2TiffConverter extends BaseAny2AnyConverter {

    public static final String TAG = "word2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOC, Format.DOCX), List.of(Format.TIFF)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public Word2TiffConverter(FileManager fileManager, TempFileService fileService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem queueItem) {
        return toTiff(queueItem);
    }

    private FileResult toTiff(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), progress, file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document word = new Document(file.getAbsolutePath());
            SmartTempFile pdfFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), "any2any", Format.PDF.getExt());
            try {
                word.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);
            } finally {
                word.cleanup();
            }
            com.aspose.pdf.Document pdf = new com.aspose.pdf.Document(pdfFile.getAbsolutePath());
            SmartTempFile tiff = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.TIFF.getExt());
            try {
                TiffDevice tiffDevice = new TiffDevice();
                tiffDevice.process(pdf, tiff.getAbsolutePath());
            } finally {
                pdf.dispose();
                pdfFile.smartDelete();
            }

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.TIFF.getExt());
            return new FileResult(fileName, tiff, stopWatch.getTime());
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

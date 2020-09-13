package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.SaveFormat;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.validator.PdfValidator;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Pdf2WordConverter extends BaseAny2AnyConverter {

    public static final String TAG = "pdf2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX));

    private TempFileService fileService;

    private FileManager fileManager;

    private PdfValidator fileValidator;

    @Autowired
    public Pdf2WordConverter(TempFileService fileService, FileManager fileManager, PdfValidator fileValidator) {
        super(MAP);
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.fileValidator = fileValidator;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            boolean validPdf = fileValidator.isValidPdf(file.getFile().getAbsolutePath());
            if (!validPdf) {
                throw new CorruptedFileException("Damaged pdf file");
            }

            return doConvert(fileQueueItem, file);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem, SmartTempFile file) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document(file.getAbsolutePath());
            try {
                SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                document.save(result.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.dispose();
            }
        } finally {
            file.smartDelete();
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case DOC:
                return SaveFormat.Doc;
            case DOCX:
                return SaveFormat.DocX;
        }

        throw new UnsupportedOperationException();
    }
}

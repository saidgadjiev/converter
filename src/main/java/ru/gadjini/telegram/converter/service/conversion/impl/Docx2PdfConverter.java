package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.PdfSaveOptions;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Docx2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "docx2pdf";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOCX), List.of(Format.PDF)
    );

    @Autowired
    public Docx2PdfConverter() {
        super(MAP);
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile docFile = getFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.DOC.getExt());
        try {
            Document docx = new Document(file.getAbsolutePath());
            try {
                docx.save(docFile.getAbsolutePath(), SaveFormat.DOC);
            } finally {
                docx.cleanup();
            }
        } catch (Throwable e) {
            docFile.smartDelete();
            throw new ConvertException(e);
        }
        try {
            Document doc = new Document(docFile.getAbsolutePath());
            try {
                SmartTempFile result = getFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                try {
                    PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
                    //pdfSaveOptions.setMemoryOptimization(true);
                    pdfSaveOptions.setOptimizeOutput(true);
                    doc.save(result.getAbsolutePath(), pdfSaveOptions);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw new ConvertException(e);
                }
            } finally {
                doc.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }
}

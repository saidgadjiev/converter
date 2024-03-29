package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.PdfSaveOptions;
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
public class DocDocx2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "doc2pdf";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOC, Format.DOCX), List.of(Format.PDF)
    );

    @Autowired
    public DocDocx2PdfConverter() {
        super(MAP);
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        try {
            Document asposeDocument = new Document(file.getAbsolutePath());
            try {
                SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
                    //pdfSaveOptions.setMemoryOptimization(true);
                    pdfSaveOptions.setOptimizeOutput(true);

                    asposeDocument.save(result.getAbsolutePath(), pdfSaveOptions);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    tempFileService().delete(result);
                    throw new ConvertException(e);
                }
            } finally {
                asposeDocument.cleanup();
            }
        } catch (Throwable ex) {
            throw new ConvertException(ex);
        }
    }
}

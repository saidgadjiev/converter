package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.PdfSaveOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.OomHandler;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Doc2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "doc2pdf";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOC), List.of(Format.PDF)
    );

    private OomHandler oomHandler;

    @Autowired
    public Doc2PdfConverter(OomHandler oomHandler) {
        super(MAP);
        this.oomHandler = oomHandler;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());
        try {
            Document asposeDocument = new Document(file.getAbsolutePath());
            try {
                SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
                    //pdfSaveOptions.setMemoryOptimization(true);
                    pdfSaveOptions.setOptimizeOutput(true);

                    asposeDocument.save(result.getAbsolutePath(), pdfSaveOptions);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw new ConvertException(e);
                }
            } finally {
                asposeDocument.cleanup();
            }
        } catch (Throwable ex) {
            if (oomHandler.isOom(ex)) {
                return oomHandler.handleOom(fileQueueItem, ex);
            }
            throw new ConvertException(ex);
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }
}

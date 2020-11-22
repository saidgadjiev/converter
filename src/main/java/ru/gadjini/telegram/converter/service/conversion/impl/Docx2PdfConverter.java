package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Docx2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "word2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOCX), List.of(Format.PDF)
    );

    @Autowired
    public Docx2PdfConverter() {
        super(MAP);
    }

    @Override
    public FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            Document docx = new Document(file.getAbsolutePath());
            try {
                SmartTempFile docFile = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.DOC.getExt());
                docx.save(docFile.getAbsolutePath(), SaveFormat.DOC);

                Document doc = new Document(docFile.getAbsolutePath());
                try {
                    SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                    try {
                        doc.save(result.getAbsolutePath(), SaveFormat.PDF);

                        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                        return new FileResult(fileName, result, null);
                    } catch (Throwable e) {
                        result.smartDelete();
                        throw e;
                    }
                } finally {
                    doc.cleanup();
                }
            } finally {
                docx.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.SaveFormat;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Xml2PdfImportConverter extends BaseAny2AnyConverter {

    private static final String TAG = "xml2pdfimport";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.XML), List.of(Format.PDF_IMPORT)
    );

    public Xml2PdfImportConverter() {
        super(MAP);
    }

    @Override
    protected ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile pdf = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            Document document = new Document(pdf.getAbsolutePath());
            try {
                document.bindXml(pdf.getAbsolutePath());
                SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF_IMPORT.getExt());
                try {
                    document.save(result.getAbsolutePath(), SaveFormat.Pdf);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.PDF_IMPORT.getExt());
                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw e;
                }
            } finally {
                document.dispose();
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

package ru.gadjini.telegram.converter.service.conversion.impl;

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
public class Pdf2XmlConverter extends BaseAny2AnyConverter {

    private static final String TAG = "pdf2xml";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.PDF), List.of(Format.XML)
    );

    public Pdf2XmlConverter() {
        super(MAP);
    }

    @Override
    protected ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile pdf = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            com.aspose.words.Document document = new com.aspose.words.Document(pdf.getAbsolutePath());
            try {
                SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.XML.getExt());
                try {
                    document.save(result.getAbsolutePath(), SaveFormat.Xml);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.XML.getExt());
                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw e;
                }
            } finally {
                document.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            pdf.smartDelete();
        }
    }
}

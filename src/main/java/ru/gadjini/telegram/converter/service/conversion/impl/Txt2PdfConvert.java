package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.service.conversion.LocalProcessExecutor;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Txt2PdfConvert extends BaseAny2AnyConverter {

    public static final String TAG = "txt2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TXT, Format.XML), List.of(Format.PDF)
    );

    private ConversionProperties conversionProperties;

    private LocalProcessExecutor localProcessExecutor;

    @Autowired
    public Txt2PdfConvert(ConversionProperties conversionProperties, LocalProcessExecutor localProcessExecutor) {
        super(MAP);
        this.conversionProperties = conversionProperties;
        this.localProcessExecutor = localProcessExecutor;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile txt = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            Document doc = new Document(txt.getAbsolutePath());
            try {
                SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                try {
                    return localProcessExecutor.execute(conversionProperties.getTimeOut(), () -> {
                        try {
                            doc.save(result.getAbsolutePath(), SaveFormat.PDF);
                        } catch (Exception e) {
                            throw new ConvertException(e);
                        }

                        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.PDF.getExt());
                        return new FileResult(fileName, result);
                    });
                } catch (Throwable e) {
                    tempFileService().delete(result);
                    throw e;
                }
            } finally {
                doc.cleanup();
            }
        } catch (ProcessTimedOutException e) {
            throw e;
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}

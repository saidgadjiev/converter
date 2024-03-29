package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.property.Url2PdfServerProperties;
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
public class Txt2WordConvert extends BaseAny2AnyConverter {

    public static final String TAG = "txt2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TXT, Format.XML), List.of(Format.DOC, Format.DOCX)
    );

    private LocalProcessExecutor localProcessExecutor;

    private Url2PdfServerProperties conversionProperties;

    @Autowired
    public Txt2WordConvert(LocalProcessExecutor localProcessExecutor, Url2PdfServerProperties conversionProperties) {
        super(MAP);
        this.localProcessExecutor = localProcessExecutor;
        this.conversionProperties = conversionProperties;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        return toWord(fileQueueItem);
    }

    private FileResult toWord(ConversionQueueItem fileQueueItem) {
        SmartTempFile txt = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            Document document = new Document(txt.getAbsolutePath());
            try {
                SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    return localProcessExecutor.execute(conversionProperties.getTimeOut(), () -> {
                        try {
                            document.save(result.getAbsolutePath(), Format.DOC.equals(fileQueueItem.getTargetFormat())
                            ? SaveFormat.DOC : SaveFormat.DOCX);
                        } catch (Exception e) {
                            throw new ConvertException(e);
                        }

                        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                        return new FileResult(fileName, result);
                    }, () -> {
                        try {
                            document.cleanup();
                        } catch (Exception e) {
                            throw new ConvertException(e);
                        }
                    });
                } catch (Throwable e) {
                    tempFileService().delete(result);
                    throw e;
                }
            } finally {
                document.cleanup();
            }
        } catch (ProcessTimedOutException e) {
            throw e;
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}

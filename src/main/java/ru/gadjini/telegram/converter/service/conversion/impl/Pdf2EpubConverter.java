package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.property.Url2PdfServerProperties;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.SmartCalibre;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.EPUB;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.PDF;

@Component
public class Pdf2EpubConverter extends BaseAny2AnyConverter {

    private static final String TAG = "pdf2epub";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(PDF), List.of(EPUB));
    }};

    private SmartCalibre calibre;

    private Url2PdfServerProperties conversionProperties;

    @Autowired
    public Pdf2EpubConverter(SmartCalibre calibre, Url2PdfServerProperties conversionProperties) {
        super(MAP);
        this.calibre = calibre;
        this.conversionProperties = conversionProperties;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        return doConvertWithCalibre(file, fileQueueItem);
    }

    private FileResult doConvertWithCalibre(SmartTempFile in, ConversionQueueItem fileQueueItem) {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            calibre.convert(in.getAbsolutePath(), result.getAbsolutePath(), conversionProperties.getTimeOut(),
                    "--title", FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, result);
        } catch (ProcessTimedOutException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}

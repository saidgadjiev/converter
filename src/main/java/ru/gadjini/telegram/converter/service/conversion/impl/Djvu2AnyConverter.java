package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.DjvuLibre;
import ru.gadjini.telegram.converter.service.conversion.device.SmartCalibre;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class Djvu2AnyConverter extends BaseAny2AnyConverter {

    private static final String TAG = "djvu2";

    private static final Logger LOGGER = LoggerFactory.getLogger(Djvu2AnyConverter.class);

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(DJVU), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP)
    );

    private SmartCalibre calibreDevice;

    private DjvuLibre djvuLibre;

    private ConversionProperties conversionProperties;

    @Autowired
    public Djvu2AnyConverter(SmartCalibre calibreDevice, DjvuLibre djvuLibre, ConversionProperties conversionProperties) {
        super(MAP);
        this.calibreDevice = calibreDevice;
        this.djvuLibre = djvuLibre;
        this.conversionProperties = conversionProperties;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile in = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            try {
                //Try convert with calibre
                calibreDevice.convert(in.getAbsolutePath(), result.getAbsolutePath(), conversionProperties.getCalibreLongConversionTimeOut());
            } catch (ProcessException ex) {
                //Fail because djvu has no actual text and has scanned images. Trying do conversion with djvulibre
                LOGGER.error("No text djvu({}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId());
                SmartTempFile outPdf = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, PDF.getExt());

                try {
                    djvuLibre.convert(in.getAbsolutePath(), outPdf.getAbsolutePath(), "-format=pdf");
                    calibreDevice.convert(outPdf.getAbsolutePath(), result.getAbsolutePath(),
                            conversionProperties.getCalibreLongConversionTimeOut(),
                            "--title", FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));
                } finally {
                    tempFileService().delete(outPdf);
                }
            }

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, result);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}

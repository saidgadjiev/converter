package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.DjvuLibre;
import ru.gadjini.telegram.converter.service.conversion.device.SmartCalibre;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
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

    @Autowired
    public Djvu2AnyConverter(SmartCalibre calibreDevice, DjvuLibre djvuLibre) {
        super(MAP);
        this.calibreDevice = calibreDevice;
        this.djvuLibre = djvuLibre;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile in = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            try {
                //Try convert with calibre
                calibreDevice.convert(in.getAbsolutePath(), out.getAbsolutePath());
            } catch (ProcessException ex) {
                //Fail because djvu has no actual text and has scanned images. Trying do conversion with djvulibre
                LOGGER.error("No text djvu({}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId());
                SmartTempFile outPdf = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, PDF.getExt());

                try {
                    djvuLibre.convert(in.getAbsolutePath(), outPdf.getAbsolutePath(), "-format=pdf");
                    calibreDevice.convert(outPdf.getAbsolutePath(), out.getAbsolutePath(), "--title", FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));
                } finally {
                    outPdf.smartDelete();
                }
            }

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, out);
        } catch (Throwable e) {
            out.smartDelete();
            throw e;
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }
}

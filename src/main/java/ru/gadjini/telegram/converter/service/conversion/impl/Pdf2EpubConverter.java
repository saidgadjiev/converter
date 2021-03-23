package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.PdfToPpmDevice;
import ru.gadjini.telegram.converter.service.conversion.device.SmartCalibre;
import ru.gadjini.telegram.converter.service.image.device.JpegEpubDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.EPUB;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.PDF;

@Component
public class Pdf2EpubConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pdf2EpubConverter.class);

    private static final String TAG = "pdf2epub";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(PDF), List.of(EPUB));
    }};

    private SmartCalibre calibre;

    private PdfToPpmDevice pdfToPpmDevice;

    private JpegEpubDevice jpegEpubDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private ConversionProperties conversionProperties;

    @Autowired
    public Pdf2EpubConverter(SmartCalibre calibre, PdfToPpmDevice pdfToPpmDevice, JpegEpubDevice jpegEpubDevice,
                             LocalisationService localisationService, UserService userService,
                             ConversionProperties conversionProperties) {
        super(MAP);
        this.calibre = calibre;
        this.pdfToPpmDevice = pdfToPpmDevice;
        this.jpegEpubDevice = jpegEpubDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.conversionProperties = conversionProperties;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            return doConvertWithCalibre(file, fileQueueItem);
        } catch (ProcessTimedOutException e) {
            return doConvertAsImages(file, fileQueueItem);
        }
    }

    private FileResult doConvertWithCalibre(SmartTempFile in, ConversionQueueItem fileQueueItem) {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            calibre.convert(in.getAbsolutePath(), result.getAbsolutePath(), conversionProperties.getCalibreLongConversionTimeOut(),
                    "--title", FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, result);
        } catch (ProcessTimedOutException e) {
            LOGGER.debug("Epub to pdf timed out({}, {})", fileQueueItem.getUserId(), fileQueueItem.getFirstFileId());
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private FileResult doConvertAsImages(SmartTempFile file, ConversionQueueItem fileQueueItem) {
        SmartTempFile tempDir = tempFileService().createTempDir(FileTarget.TEMP, fileQueueItem.getUserId(), TAG);
        try {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), TAG, EPUB.getExt());
            try {
                pdfToPpmDevice.convert(file.getAbsolutePath(), tempDir.getAbsolutePath() + File.separator + "p",
                        "-jpeg", "-jpegopt", "quality=70");

                jpegEpubDevice.convert(tempDir.getAbsolutePath() + File.separator, result.getAbsolutePath(), "--title", "\"Epub\"");

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                String caption = localisationService.getMessage(MessagesProperties.MESSAGE_PDF_IMAGES_EPUB,
                        userService.getLocaleOrDefault(fileQueueItem.getUserId()));
                return new FileResult(fileName, result, caption);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw new ConvertException(e);
            }
        } finally {
            tempFileService().delete(tempDir);
        }
    }
}

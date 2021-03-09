package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.P7ArchiveDevice;
import ru.gadjini.telegram.converter.service.conversion.device.PdfToPpmDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.List;
import java.util.Map;


@Component
public class Pdf2PngJpgConverter extends BaseAny2AnyConverter {

    public static final String TAG = "pdf2pngjpg";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.PNG, Format.JPG));

    private PdfToPpmDevice pdfToPpmDevice;

    private P7ArchiveDevice p7ArchiveDevice;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public Pdf2PngJpgConverter(PdfToPpmDevice pdfToPpmDevice, P7ArchiveDevice p7ArchiveDevice,
                               LocalisationService localisationService, UserService userService) {
        super(MAP);
        this.pdfToPpmDevice = pdfToPpmDevice;
        this.p7ArchiveDevice = p7ArchiveDevice;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            SmartTempFile tempDir = tempFileService().createTempDir(FileTarget.TEMP, fileQueueItem.getUserId(), TAG);
            try {
                pdfToPpmDevice.convert(file.getAbsolutePath(), tempDir.getAbsolutePath() + File.separator + "p", getOptions(fileQueueItem.getTargetFormat()));

                File[] files = tempDir.listFiles();
                if (files != null && files.length == 1) {
                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, new SmartTempFile(files[0], true));
                } else {
                    SmartTempFile result = tempFileService().getTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                            fileQueueItem.getFirstFileId(), TAG, Format.ZIP.getExt());
                    try {
                        p7ArchiveDevice.zip(tempDir.getAbsolutePath() + File.separator + "*", result.getAbsolutePath());
                        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.ZIP.getExt());

                        String caption = localisationService.getMessage(MessagesProperties.MESSAGE_PDF_IMAGES_UNZIP_BOT, userService.getLocaleOrDefault(fileQueueItem.getUserId()));
                        return new FileResult(fileName, result, caption);
                    } catch (Throwable e) {
                        tempFileService().delete(result);
                        throw e;
                    }
                }
            } finally {
                tempFileService().delete(tempDir);
            }
        } catch (ProcessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        tempFileService().delete(fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()));
    }

    private String[] getOptions(Format format) {
        if (format == Format.PNG) {
            return new String[]{
                    "-png"
            };
        }

        return new String[]{
                "-jpeg", "-jpegopt", "quality=100"
        };
    }
}

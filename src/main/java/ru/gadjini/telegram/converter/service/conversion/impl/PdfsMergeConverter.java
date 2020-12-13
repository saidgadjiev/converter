package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.PdfUniteDevice;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PdfsMergeConverter extends BaseAny2AnyConverter {

    private static final String TAG = "pdfsmerge";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.PDFS), List.of(Format.MERGE_PDFS)
    );

    private PdfUniteDevice pdfUniteDevice;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public PdfsMergeConverter(PdfUniteDevice pdfUniteDevice, UserService userService, LocalisationService localisationService) {
        super(MAP);
        this.pdfUniteDevice = pdfUniteDevice;
        this.userService = userService;
        this.localisationService = localisationService;
    }

    @Override
    protected ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        List<SmartTempFile> pdfs = fileQueueItem.getDownloadedFiles()
                .stream()
                .map(downloadingQueueItem -> new SmartTempFile(new File(downloadingQueueItem.getFilePath()), downloadingQueueItem.isDeleteParentDir()))
                .collect(Collectors.toList());

        try {
            SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), TAG, Format.PDF.getExt());
            try {
                Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                pdfUniteDevice.mergePdfs(pdfs.stream().map(SmartTempFile::getAbsolutePath).collect(Collectors.toList()), result.getAbsolutePath());

                String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + "." + Format.PDF.getExt();
                return new FileResult(fileName, result);
            } catch (Throwable e) {
                result.smartDelete();
                throw e;
            }
        } finally {
            pdfs.forEach(SmartTempFile::smartDelete);
        }
    }
}

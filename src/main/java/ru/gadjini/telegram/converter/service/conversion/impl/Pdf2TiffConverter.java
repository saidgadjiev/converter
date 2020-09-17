package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.devices.TiffDevice;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.validator.PdfValidator;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Pdf2TiffConverter extends BaseAny2AnyConverter {

    public static final String TAG = "pdf2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.TIFF));

    private TempFileService fileService;

    private FileManager fileManager;

    private PdfValidator fileValidator;

    @Autowired
    public Pdf2TiffConverter(TempFileService fileService, FileManager fileManager, PdfValidator fileValidator) {
        super(MAP);
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.fileValidator = fileValidator;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getFirstSize(), progress, file);
            boolean validPdf = fileValidator.isValidPdf(file.getFile().getAbsolutePath());
            if (!validPdf) {
                throw new CorruptedFileException("Damaged pdf file");
            }
            return toTiff(fileQueueItem, file);

        } finally {
            file.smartDelete();
        }
    }

    private FileResult toTiff(ConversionQueueItem fileQueueItem, SmartTempFile pdfFile) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document pdf = new Document(pdfFile.getAbsolutePath());
            try {
                TiffDevice tiffDevice = new TiffDevice();
                SmartTempFile tiff = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.TIFF.getExt());
                tiffDevice.process(pdf, tiff.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.TIFF.getExt());
                return new FileResult(fileName, tiff, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                pdf.dispose();
            }
        } finally {
            pdfFile.smartDelete();
        }
    }
}

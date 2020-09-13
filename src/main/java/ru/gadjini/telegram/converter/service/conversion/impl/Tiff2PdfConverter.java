package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.ImageConvertDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Tiff2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "tiff2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TIFF), List.of(Format.PDF)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    @Autowired
    public Tiff2PdfConverter(FileManager fileManager, TempFileService fileService, ImageConvertDevice imageDevice) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile tiff = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());
        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), tiff);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile pdf = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PDF.getExt());
            imageDevice.convert(tiff.getAbsolutePath(), pdf.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
            return new FileResult(fileName, pdf, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            tiff.smartDelete();
        }
    }
}

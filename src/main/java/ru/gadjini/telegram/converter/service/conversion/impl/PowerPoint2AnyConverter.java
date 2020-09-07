package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class PowerPoint2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    public static final String TAG = "pp2";

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.PPTX,
            Format.PPT,
            Format.PPTM,
            Format.POTX,
            Format.POT,
            Format.POTM,
            Format.PPS,
            Format.PPSX,
            Format.PPSM);

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public PowerPoint2AnyConverter(FileManager fileManager, TempFileService fileService, ConversionFormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Presentation presentation = new Presentation(file.getAbsolutePath());
            try {
                SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PDF.getExt());
                presentation.save(tempFile.getAbsolutePath(), SaveFormat.Pdf);

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
                return new FileResult(fileName, tempFile, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                presentation.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

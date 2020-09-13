package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Excel2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "excel2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.XLS, Format.XLSX), List.of(Format.PDF));

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public Excel2AnyConverter(FileManager fileManager, TempFileService fileService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Workbook workbook = new Workbook(file.getAbsolutePath());
            try {
                SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PDF.getExt());
                workbook.save(tempFile.getAbsolutePath(), SaveFormat.PDF);

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
                return new FileResult(fileName, tempFile, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                workbook.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

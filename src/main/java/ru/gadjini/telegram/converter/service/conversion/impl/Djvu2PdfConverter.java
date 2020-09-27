package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.DjvuLibre;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.DJVU;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.PDF;

@Component
public class Djvu2PdfConverter extends BaseAny2AnyConverter {

    private static final String TAG = "djvu2pdf";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(DJVU), List.of(PDF)
    );

    private DjvuLibre djvuLibre;

    private FileManager fileManager;

    private TempFileService tempFileService;

    @Autowired
    public Djvu2PdfConverter(DjvuLibre djvuLibre, FileManager fileManager, TempFileService tempFileService) {
        super(MAP);
        this.djvuLibre = djvuLibre;
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile in = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, in);

            SmartTempFile file = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            djvuLibre.convert(in.getAbsolutePath(), file.getAbsolutePath(), "-format=pdf");

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, file);
        } finally {
            in.smartDelete();
        }
    }
}

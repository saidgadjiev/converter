package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.ConvertDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
@SuppressWarnings("CPD-START")
public class Comic2AnyConverter extends BaseAny2AnyConverter {

    private static final String TAG = "calibre2";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(CBZ), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(CBR), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(CBC), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
    }};

    private ConvertDevice convertDevice;

    private FileManager fileManager;

    private TempFileService tempFileService;

    @Autowired
    public Comic2AnyConverter(ConvertDevice convertDevice, FileManager fileManager, TempFileService tempFileService) {
        super(MAP);
        this.convertDevice = convertDevice;
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile in = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getFirstSize(), progress, in);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile file = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

            convertDevice.convert(in.getAbsolutePath(), file.getAbsolutePath(), FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()), "--dont-grayscale", "--landscape", "--no-sort", "--disable-trim");

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, file, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            in.smartDelete();
        }
    }
}

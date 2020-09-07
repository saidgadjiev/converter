package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.ConvertDevice;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class CalibreFormatsConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "calibre2";

    private ConvertDevice convertDevice;

    private FileManager fileManager;

    private TempFileService tempFileService;

    @Autowired
    public CalibreFormatsConverter(ConversionFormatService formatService, ConvertDevice convertDevice, FileManager fileManager,
                                   TempFileService tempFileService) {
        super(Set.of(AZW, AZW3, AZW4, CBZ, CBR, CBC, CHM, DJVU, EPUB, FB2, FBZ, HTMLZ, LIT, LRF, MOBI, ODT,
                PRC, PDB, PML, RB, RTF, SNB, TCR, TXTZ), formatService);
        this.convertDevice = convertDevice;
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile in = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), in);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile file = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

            convertDevice.convert(in.getAbsolutePath(), file.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, file, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            in.smartDelete();
        }
    }
}

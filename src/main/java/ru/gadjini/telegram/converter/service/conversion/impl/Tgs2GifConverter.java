package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.archive.ArchiveService;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Tgs2GifConverter extends BaseAny2AnyConverter {

    public static final String TAG = "tgs2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TGS), List.of(Format.GIF)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    private ArchiveService archiveService;

    @Autowired
    public Tgs2GifConverter(FileManager fileManager, TempFileService fileService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Autowired
    public void setArchiveService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        return toGiff(fileQueueItem);
    }

    private FileResult toGiff(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.GIF.getExt());
            try {
                new ProcessExecutor().execute(command(file.getAbsolutePath(), result.getAbsolutePath()));
                SmartTempFile archive = archiveService.createArchive(fileQueueItem.getUserId(), List.of(result.getFile()), Format.ZIP);

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.GIF.getExt());
                return new FileResult(fileName, archive, stopWatch.getTime(TimeUnit.SECONDS));
            } catch (Exception ex) {
                result.smartDelete();
                throw ex;
            }
        } finally {
            file.smartDelete();
        }
    }

    private String[] command(String in, String out) {
        return new String[]{"node", "tgs-to-gif/cli.js", in, "--out", out};
    }
}

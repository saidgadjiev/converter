package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class FFmpegFormatsConverter extends BaseAny2AnyConverter {

    private static final String TAG = "ffmpeg";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.MP4), List.of(Format._3GP),
            List.of(Format._3GP), List.of(Format.MP4)
    );

    private FFmpegDevice fFmpegDevice;

    private TempFileService fileService;

    private FileManager fileManager;

    @Autowired
    public FFmpegFormatsConverter(FFmpegDevice fFmpegDevice, TempFileService fileService, FileManager fileManager) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fileService = fileService;
        this.fileManager = fileManager;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile out = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, out, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            file.smartDelete();
        }
    }
}

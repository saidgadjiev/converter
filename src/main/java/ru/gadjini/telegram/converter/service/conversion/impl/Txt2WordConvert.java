package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.TxtLoadOptions;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
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
public class Txt2WordConvert extends BaseAny2AnyConverter {

    public static final String TAG = "txt2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TXT), List.of(Format.DOC, Format.DOCX)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public Txt2WordConvert(FileManager fileManager, TempFileService fileService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        return toWord(fileQueueItem);
    }

    private FileResult toWord(ConversionQueueItem fileQueueItem) {
        SmartTempFile txt = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, txt);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            com.aspose.words.Document document = new com.aspose.words.Document(txt.getAbsolutePath(), new TxtLoadOptions());
            try {
                SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                document.save(result.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            txt.smartDelete();
        }
    }
}

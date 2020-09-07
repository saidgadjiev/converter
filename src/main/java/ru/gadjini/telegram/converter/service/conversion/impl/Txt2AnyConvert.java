package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.TextFragment;
import com.aspose.words.TxtLoadOptions;
import com.google.common.io.Files;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Txt2AnyConvert extends BaseAny2AnyConverter<FileResult> {

    public static final String TAG = "txt2";

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public Txt2AnyConvert(ConversionFormatService formatService, FileManager fileManager, TempFileService fileService) {
        super(Set.of(Format.TXT), formatService);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        return toWord(fileQueueItem);
    }

    private FileResult toWord(ConversionQueueItem fileQueueItem) {
        SmartTempFile txt = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), txt);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            com.aspose.words.Document document = new com.aspose.words.Document(txt.getAbsolutePath(), new TxtLoadOptions());
            try {
                SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                document.save(result.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
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

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile txt = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), txt);
            List<String> lines = Files.readLines(txt.getFile(), StandardCharsets.UTF_8);
            StringBuilder builder = new StringBuilder();
            lines.forEach(builder::append);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Document doc = new Document();
            try {
                Page page = doc.getPages().add();
                TextFragment text = new TextFragment(builder.toString());

                page.getParagraphs().add(text);

                SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.PDF.getExt());
                doc.save(result.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                doc.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            txt.smartDelete();
        }
    }
}

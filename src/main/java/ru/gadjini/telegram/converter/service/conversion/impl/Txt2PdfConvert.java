package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.TextFragment;
import com.google.common.io.Files;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class Txt2PdfConvert extends BaseAny2AnyConverter {

    public static final String TAG = "txt2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TXT), List.of(Format.PDF)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public Txt2PdfConvert(FileManager fileManager, TempFileService fileService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile txt = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, txt);
            List<String> lines = Files.readLines(txt.getFile(), StandardCharsets.UTF_8);
            StringBuilder builder = new StringBuilder();
            lines.forEach(builder::append);

            Document doc = new Document();
            try {
                Page page = doc.getPages().add();
                TextFragment text = new TextFragment(builder.toString());

                page.getParagraphs().add(text);

                SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                try {
                    doc.save(result.getAbsolutePath());

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.PDF.getExt());
                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw e;
                }
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

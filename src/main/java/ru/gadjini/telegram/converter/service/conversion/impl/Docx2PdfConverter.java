package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Docx2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "word2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOCX), List.of(Format.PDF)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public Docx2PdfConverter(FileManager fileManager, TempFileService fileService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem queueItem) {
        return doConvert(queueItem);
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.forceDownloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);

            Document docx = new Document(file.getAbsolutePath());
            try {
                SmartTempFile docFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.DOC.getExt());
                docx.save(docFile.getAbsolutePath(), SaveFormat.DOC);

                Document doc = new Document(docFile.getAbsolutePath());
                try {
                    SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                    try {
                        doc.save(result.getAbsolutePath(), SaveFormat.PDF);

                        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                        return new FileResult(fileName, result);
                    } catch (Throwable e) {
                        result.smartDelete();
                        throw e;
                    }
                } finally {
                    doc.cleanup();
                }
            } finally {
                docx.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

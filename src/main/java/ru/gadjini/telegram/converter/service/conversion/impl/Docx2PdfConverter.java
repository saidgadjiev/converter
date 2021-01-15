package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.PdfSaveOptions;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposeExecutorService;
import ru.gadjini.telegram.converter.service.conversion.device.BusyConvertResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class Docx2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "docx2pdf";

    private static final int LIGHT_FILE_PAGES = 120;

    private AsposeExecutorService asposeExecutorService;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOCX), List.of(Format.PDF)
    );

    @Autowired
    public Docx2PdfConverter(AsposeExecutorService asposeExecutorService) {
        super(MAP);
        this.asposeExecutorService = asposeExecutorService;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        AtomicReference<FileResult> fileResultAtomicReference = new AtomicReference<>();
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile docFile = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.DOC.getExt());
        try {
            Document docx = new Document(file.getAbsolutePath());
            try {
                docx.save(docFile.getAbsolutePath(), SaveFormat.DOC);
            } finally {
                docx.cleanup();
            }
        } catch (Throwable e) {
            docFile.smartDelete();
            throw new ConvertException(e);
        }
        try {
            Document doc = new Document(docFile.getAbsolutePath());
            try {
                CompletableFuture<Boolean> completableFuture = asposeExecutorService.submit(new AsposeExecutorService.AsposeTask() {
                    @Override
                    public int getId() {
                        return fileQueueItem.getId();
                    }

                    @Override
                    public SmartExecutorService.JobWeight getWeight() {
                        try {
                            return doc.getPageCount() > LIGHT_FILE_PAGES ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
                        } catch (Exception e) {
                            throw new ConvertException(e);
                        }
                    }

                    @Override
                    public void run() {
                        SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                        try {
                            PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
                            pdfSaveOptions.setMemoryOptimization(true);
                            pdfSaveOptions.setOptimizeOutput(true);
                            doc.save(result.getAbsolutePath(), pdfSaveOptions);

                            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                            fileResultAtomicReference.set(new FileResult(fileName, result));
                        } catch (Throwable e) {
                            result.smartDelete();
                            throw new ConvertException(e);
                        }
                    }
                });

                try {
                    Boolean aBoolean = completableFuture.get();

                    if (aBoolean) {
                        return fileResultAtomicReference.get();
                    }

                    return new BusyConvertResult();
                } catch (Exception e) {
                    throw new ConvertException(e);
                }
            } finally {
                doc.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

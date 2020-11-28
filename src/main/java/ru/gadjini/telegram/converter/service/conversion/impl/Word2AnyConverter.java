package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.OomHandler;
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
public class Word2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "word2";

    private static final int LIGHT_FILE_PAGES = 120;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOC), List.of(Format.DOCX, Format.PDF, Format.TXT),
            List.of(Format.DOCX), List.of(Format.DOC, Format.TXT)
    );

    private AsposeExecutorService asposeExecutorService;

    private OomHandler oomHandler;

    @Autowired
    public Word2AnyConverter(AsposeExecutorService asposeExecutorService, OomHandler oomHandler) {
        super(MAP);
        this.asposeExecutorService = asposeExecutorService;
        this.oomHandler = oomHandler;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        AtomicReference<FileResult> fileResultAtomicReference = new AtomicReference<>();

        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());
        try {
            Document asposeDocument = new Document(file.getAbsolutePath());
            try {
                CompletableFuture<Boolean> completableFuture = asposeExecutorService.submit(new AsposeExecutorService.AsposeTask() {
                    @Override
                    public int getId() {
                        return fileQueueItem.getId();
                    }

                    @Override
                    public SmartExecutorService.JobWeight getWeight() {
                        try {
                            return asposeDocument.getPageCount() > LIGHT_FILE_PAGES ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
                        } catch (Exception e) {
                            throw new ConvertException(e);
                        }
                    }

                    @Override
                    public void run() {
                        SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                        try {
                            SmartTempFile tempDir = getFileService().createTempDir(fileQueueItem.getUserId(), TAG);

                            try {
                                if (fileQueueItem.getTargetFormat() == Format.PDF) {
                                    PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
                                    pdfSaveOptions.setMemoryOptimization(true);
                                    pdfSaveOptions.setOptimizeOutput(true);
                                    pdfSaveOptions.setTempFolder(tempDir.getAbsolutePath());

                                    asposeDocument.save(result.getAbsolutePath(), pdfSaveOptions);
                                } else if (fileQueueItem.getTargetFormat() == Format.DOC) {
                                    DocSaveOptions docSaveOptions = new DocSaveOptions();
                                    docSaveOptions.setMemoryOptimization(true);
                                    docSaveOptions.setTempFolder(tempDir.getAbsolutePath());

                                    asposeDocument.save(result.getAbsolutePath(), docSaveOptions);
                                } else if (fileQueueItem.getTargetFormat() == Format.TXT) {
                                    TxtSaveOptions txtSaveOptions = new TxtSaveOptions();
                                    txtSaveOptions.setMemoryOptimization(true);
                                    txtSaveOptions.setTempFolder(tempDir.getAbsolutePath());

                                    asposeDocument.save(result.getAbsolutePath(), txtSaveOptions);
                                } else {
                                    asposeDocument.save(result.getAbsolutePath(), SaveFormat.DOCX);
                                }
                            } finally {
                                tempDir.smartDelete();
                            }

                            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                            fileResultAtomicReference.set(new FileResult(fileName, result, null));
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
                asposeDocument.cleanup();
            }
        } catch (Throwable ex) {
            if (oomHandler.isOom(ex)) {
                return oomHandler.handleOom(fileQueueItem, ex);
            }
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.*;
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
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class Word2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "word2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOC), List.of(Format.DOCX, Format.PDF, Format.TXT),
            List.of(Format.DOCX), List.of(Format.DOC, Format.TXT)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    private AsposeExecutorService asposeExecutorService;

    @Autowired
    public Word2AnyConverter(FileManager fileManager, TempFileService fileService, AsposeExecutorService asposeExecutorService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.asposeExecutorService = asposeExecutorService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem queueItem) {
        return doConvert(queueItem);
    }

    private ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        AtomicReference<FileResult> fileResultAtomicReference = new AtomicReference<>();

        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);

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
                            return asposeDocument.getPageCount() > 150 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
                        } catch (Exception e) {
                            throw new ConvertException(e);
                        }
                    }

                    @Override
                    public void run() {
                        SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                        try {
                            SmartTempFile tempDir = fileService.createTempDir(fileQueueItem.getUserId(), TAG);

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
                asposeDocument.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

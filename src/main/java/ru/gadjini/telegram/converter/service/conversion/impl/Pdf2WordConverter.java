package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.DocSaveOptions;
import com.aspose.pdf.Document;
import com.aspose.pdf.SaveFormat;
import com.aspose.pdf.UnifiedSaveOptions;
import com.aspose.pdf.facades.PdfFileEditor;
import com.aspose.words.ConvertUtil;
import com.aspose.words.ImportFormatMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposeExecutorService;
import ru.gadjini.telegram.converter.service.conversion.device.BusyConvertResult;
import ru.gadjini.telegram.converter.service.logger.FileLg;
import ru.gadjini.telegram.converter.service.logger.Lg;
import ru.gadjini.telegram.converter.service.logger.SoutLg;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class Pdf2WordConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pdf2WordConverter.class);

    private static final int LIGHT_FILE_PAGES = 150;

    public static final String TAG = "pdf2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX));

    private ProcessExecutor processExecutor;

    private AsposeExecutorService asposeExecutorService;

    @Autowired
    public Pdf2WordConverter(ProcessExecutor processExecutor,
                             AsposeExecutorService asposeExecutorService) {
        super(MAP);
        this.processExecutor = processExecutor;
        this.asposeExecutorService = asposeExecutorService;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());
        File logFile = processExecutor.getErrorLogFile();

        ConvertResult fileResult;
        boolean dirtyConvert = false;

        try (Lg log = logFile == null ? new SoutLg() : new FileLg(logFile)) {
            LOGGER.debug("Log file({}, {})", fileQueueItem.getId(), logFile == null ? "sout" : logFile.getAbsolutePath());
            log.log("Start pdf 2 word(%s, %s, %s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(), fileQueueItem.getTargetFormat());

            try {
                try {
                    fileResult = doRightConvert(fileQueueItem, file, log);
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                    log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));

                    dirtyConvert = true;
                    fileResult = doDirtyConvert(fileQueueItem, file, log);
                }
            } catch (Throwable e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw new ProcessException(-1, e.getMessage() + "\n Log gile:" + (logFile != null ? logFile.getAbsolutePath() : "sout") + "\n" + ExceptionUtils.getStackTrace(e));
            } finally {
                file.smartDelete();
            }
        }

        if (!dirtyConvert && logFile != null) {
            FileUtils.deleteQuietly(logFile);
            if (logFile.exists()) {
                LOGGER.debug("Log file not deleted({})", logFile.getAbsolutePath());
            }
        }

        return fileResult;
    }

    public ConvertResult convert(ConversionQueueItem fileQueueItem, SmartTempFile file) {
        File logFile = processExecutor.getErrorLogFile();

        ConvertResult fileResult;

        try (Lg log = logFile == null ? new SoutLg() : new FileLg(logFile)) {
            LOGGER.debug("Log file({}, {})", fileQueueItem.getId(), logFile == null ? "sout" : logFile.getAbsolutePath());
            log.log("Start pdf 2 word(%s, %s, %s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(), fileQueueItem.getTargetFormat());

            try {
                try {
                    fileResult = doRightConvert(fileQueueItem, file, log);
                } catch (OutOfMemoryError e) {
                    throw e;
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                    log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));

                    fileResult = doDirtyConvert(fileQueueItem, file, log);
                }
            } catch (Throwable e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw new ProcessException(-1, e.getMessage() + "\n Log gile:" + (logFile != null ? logFile.getAbsolutePath() : "sout") + "\n" + ExceptionUtils.getStackTrace(e));
            }
        }

        if (logFile != null) {
            FileUtils.deleteQuietly(logFile);
            if (logFile.exists()) {
                LOGGER.debug("Log file not deleted({})", logFile.getAbsolutePath());
            }
        }

        return fileResult;
    }

    private ConvertResult doRightConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) throws Exception {
        AtomicReference<FileResult> fileResultAtomicReference = new AtomicReference<>();

        Document document = new Document(file.getAbsolutePath());
        try {
            CompletableFuture<Boolean> completableFuture = asposeExecutorService.submit(new AsposeExecutorService.AsposeTask() {
                @Override
                public int getId() {
                    return fileQueueItem.getId();
                }

                @Override
                public SmartExecutorService.JobWeight getWeight() {
                    return Pdf2WordConverter.this.getWeight(document.getPages().size());
                }

                @Override
                public void run() {
                    SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                    try {
                        document.optimize();
                        document.optimizeResources();
                        DocSaveOptions docSaveOptions = new DocSaveOptions();
                        docSaveOptions.setFormat(fileQueueItem.getTargetFormat() == Format.DOC ? DocSaveOptions.DocFormat.Doc : DocSaveOptions.DocFormat.DocX);
                        docSaveOptions.setMode(DocSaveOptions.RecognitionMode.Textbox);
                        docSaveOptions.CustomProgressHandler = new UnifiedSaveOptions.ConversionProgressEventHandler() {

                            @Override
                            public void invoke(UnifiedSaveOptions.ProgressEventHandlerInfo progressEventHandlerInfo) {
                                log.log("EventType: " + progressEventHandlerInfo.EventType + ", Value: " +
                                        progressEventHandlerInfo.Value + ", MaxValue: " + progressEventHandlerInfo.MaxValue);
                            }
                        };
                        document.save(result.getAbsolutePath(), docSaveOptions);

                        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                        fileResultAtomicReference.set(new FileResult(fileName, result, null));
                    } catch (Throwable e) {
                        result.smartDelete();
                        throw e;
                    }
                }
            });

            Boolean aBoolean = completableFuture.get();

            if (aBoolean) {
                return fileResultAtomicReference.get();
            }
        } finally {
            document.dispose();
        }

        return new BusyConvertResult();
    }

    private ConvertResult doDirtyConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) throws Exception {
        log.log("Start dirty way pdf 2 word(%s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId());
        AtomicReference<FileResult> fileResultAtomicReference = new AtomicReference<>();

        Document document = new Document(file.getAbsolutePath());
        try {
            CompletableFuture<Boolean> completableFuture = asposeExecutorService.submit(new AsposeExecutorService.AsposeTask() {
                @Override
                public int getId() {
                    return fileQueueItem.getId();
                }

                @Override
                public SmartExecutorService.JobWeight getWeight() {
                    return Pdf2WordConverter.this.getWeight(document.getPages().size());
                }

                @Override
                public void run() {
                    SmartTempFile tempDir = getFileService().createTempDir(fileQueueItem.getUserId(), TAG);
                    try {
                        SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

                        try {
                            log.log("Slit dir: " + tempDir.getAbsolutePath());

                            PdfFileEditor pdfFileEditor = new PdfFileEditor();
                            pdfFileEditor.splitToPages(file.getAbsolutePath(), tempDir.getAbsolutePath() + File.separator + "%NUM%.pdf");
                            List<File> files = Arrays.asList(tempDir.listFiles());
                            files.sort(Comparator.comparingInt(o -> Integer.parseInt(FilenameUtils.getBaseName(o.getName()))));
                            log.log("Pages: " + files.size());

                            String firstWord = tempDir.getAbsolutePath() + File.separator + "1." + Format.DOC.getExt();
                            Document firstPdf = new Document(files.get(0).getAbsolutePath());
                            double width = firstPdf.getPageInfo().getWidth();
                            double height = firstPdf.getPageInfo().getHeight();

                            try {
                                firstPdf.save(firstWord, SaveFormat.Doc);
                            } finally {
                                firstPdf.dispose();
                            }
                            com.aspose.words.Document destWord = new com.aspose.words.Document(firstWord);

                            log.log("Start word initialized");
                            try {
                                for (int i = 1; i < files.size(); ++i) {
                                    int page = i + 1;
                                    log.log("Start " + page + "-th page");
                                    String wordPath = tempDir.getAbsolutePath() + File.separator + page + "." + Format.DOC.getExt();
                                    Document pdf = new Document(files.get(i).getAbsolutePath());
                                    try {
                                        pdf.save(wordPath, SaveFormat.Doc);
                                    } catch (Throwable e) {
                                        log.log("Skip " + page + "\n%s\n", e.getMessage(), ExceptionUtils.getStackTrace(e));
                                    } finally {
                                        pdf.dispose();
                                    }

                                    log.log("Saved to pdf " + page + "-th page");
                                    com.aspose.words.Document document = new com.aspose.words.Document(wordPath);
                                    try {
                                        destWord.appendDocument(document, ImportFormatMode.USE_DESTINATION_STYLES);
                                    } finally {
                                        document.cleanup();
                                    }
                                    log.log("Processed " + page + "-th page");
                                }

                                destWord.getSections().forEach(nodes -> {
                                    nodes.getPageSetup().setPageWidth(width);
                                    nodes.getPageSetup().setPageHeight(height);

                                    nodes.getPageSetup().setLeftMargin(ConvertUtil.millimeterToPoint(30));
                                    nodes.getPageSetup().setBottomMargin(ConvertUtil.millimeterToPoint(20));
                                    nodes.getPageSetup().setRightMargin(ConvertUtil.millimeterToPoint(15));
                                    nodes.getPageSetup().setTopMargin(ConvertUtil.millimeterToPoint(20));
                                });
                                destWord.updatePageLayout();
                                destWord.save(result.getAbsolutePath(), getWordSaveFormat(fileQueueItem.getTargetFormat()));
                            } finally {
                                destWord.cleanup();
                            }

                            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                            fileResultAtomicReference.set(new FileResult(fileName, result, null));
                        } catch (Throwable e) {
                            result.smartDelete();
                            throw new ConvertException(e);
                        }
                    } finally {
                        tempDir.smartDelete();
                    }
                }
            });
            Boolean aBoolean = completableFuture.get();

            if (aBoolean) {
                return fileResultAtomicReference.get();
            }
        } finally {
            document.dispose();
        }

        return new BusyConvertResult();
    }

    private int getWordSaveFormat(Format format) {
        switch (format) {
            case DOC:
                return com.aspose.words.SaveFormat.DOC;
            case DOCX:
                return com.aspose.words.SaveFormat.DOCX;
        }

        throw new UnsupportedOperationException();
    }

    private SmartExecutorService.JobWeight getWeight(int pages) {
        return pages > LIGHT_FILE_PAGES ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
    }
}

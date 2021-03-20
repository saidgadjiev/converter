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
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.service.conversion.OomHandler;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.BusyConvertResult;
import ru.gadjini.telegram.converter.service.logger.FileLg;
import ru.gadjini.telegram.converter.service.logger.Lg;
import ru.gadjini.telegram.converter.service.logger.SoutLg;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class Pdf2WordConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pdf2WordConverter.class);

    public static final String TAG = "pdf2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX));

    private ProcessExecutor processExecutor;

    private OomHandler oomHandler;

    private ConversionProperties conversionProperties;

    @Autowired
    public Pdf2WordConverter(ProcessExecutor processExecutor, OomHandler oomHandler, ConversionProperties conversionProperties) {
        super(MAP);
        this.processExecutor = processExecutor;
        this.oomHandler = oomHandler;
        this.conversionProperties = conversionProperties;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());
        File logFile = processExecutor.getErrorLogFile();

        ConversionResult fileResult;
        boolean dirtyConvert = false;

        try (Lg log = logFile == null ? new SoutLg() : new FileLg(logFile)) {
            LOGGER.debug("Log file({}, {})", fileQueueItem.getId(), logFile == null ? "sout" : logFile.getAbsolutePath());
            log.log("Start pdf 2 word(%s, %s, %s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(), fileQueueItem.getTargetFormat());

            try {
                try {
                    fileResult = doRightConvert(fileQueueItem, file, log);
                } catch (ProcessTimedOutException e) {
                    throw e;
                } catch (Throwable e) {
                    if (oomHandler.isOom(e)) {
                        return oomHandler.handleOom(fileQueueItem, e);
                    }
                    LOGGER.error(e.getMessage(), e);
                    log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));

                    dirtyConvert = true;
                    fileResult = doDirtyConvert(fileQueueItem, file, log);
                }
            } catch (ProcessTimedOutException e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw e;
            } catch (Throwable e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw new ProcessException(-1, e.getMessage() + "\n Log gile:" + (logFile != null ? logFile.getAbsolutePath() : "sout") + "\n" + ExceptionUtils.getStackTrace(e));
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

    private ConversionResult doRightConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) {
        Document document = new Document(file.getAbsolutePath());
        try {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                CompletableFuture<ConversionResult> feature = CompletableFuture.supplyAsync(() -> {
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

                    return new FileResult(fileName, result);
                });

                try {
                    return feature.get(conversionProperties.getPdfToWordLongConversionTimeOut(), TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    feature.cancel(true);
                    throw new ProcessTimedOutException();
                } catch (Exception e) {
                    throw new ProcessException(e);
                }
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw e;
            }
        } finally {
            document.dispose();
        }
    }

    private ConversionResult doDirtyConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) {
        log.log("Start dirty way pdf 2 word(%s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId());
        AtomicReference<FileResult> fileResultAtomicReference = new AtomicReference<>();

        Document document = new Document(file.getAbsolutePath());
        try {
            SmartTempFile tempDir = tempFileService().createTempDir(FileTarget.TEMP, fileQueueItem.getUserId(), TAG);
            try {
                SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

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
                            com.aspose.words.Document wordDoc = new com.aspose.words.Document(wordPath);
                            try {
                                destWord.appendDocument(wordDoc, ImportFormatMode.USE_DESTINATION_STYLES);
                            } finally {
                                wordDoc.cleanup();
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
                    fileResultAtomicReference.set(new FileResult(fileName, result));
                } catch (Throwable e) {
                    tempFileService().delete(result);
                    throw new ConvertException(e);
                }
            } finally {
                tempFileService().delete(tempDir);
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
}

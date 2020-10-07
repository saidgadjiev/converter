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
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.validator.PdfValidator;
import ru.gadjini.telegram.converter.service.logger.FileLg;
import ru.gadjini.telegram.converter.service.logger.Lg;
import ru.gadjini.telegram.converter.service.logger.SoutLg;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class Pdf2WordConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pdf2WordConverter.class);

    public static final String TAG = "pdf2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX));

    private TempFileService fileService;

    private FileManager fileManager;

    private PdfValidator fileValidator;

    private ProcessExecutor processExecutor;

    private ConversionQueueService queueService;

    @Autowired
    public Pdf2WordConverter(TempFileService fileService, FileManager fileManager, PdfValidator fileValidator,
                             ProcessExecutor processExecutor, ConversionQueueService queueService) {
        super(MAP);
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.fileValidator = fileValidator;
        this.processExecutor = processExecutor;
        this.queueService = queueService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        File logFile = processExecutor.getErrorLogFile();

        FileResult fileResult;
        boolean dirtyConvert = false;

        try (Lg log = logFile == null ? new SoutLg() : new FileLg(logFile)) {
            LOGGER.debug("Log file({}, {})", fileQueueItem.getId(), logFile == null ? "sout" : logFile.getAbsolutePath());
            log.log("Start pdf 2 word(%s, %s, %s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(), fileQueueItem.getTargetFormat());

            try {
                Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
                fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);
                boolean validPdf = fileValidator.isValidPdf(file.getFile().getAbsolutePath());
                if (!validPdf) {
                    throw new CorruptedFileException("Damaged pdf file");
                }

                try {
                    fileResult = doRightConvert(fileQueueItem, file, log);
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                    log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));

                    dirtyConvert = true;
                    queueService.exception(fileQueueItem.getId(), e);
                    fileResult = doDirtyConvert(fileQueueItem, file, log);
                }
            } catch (Throwable e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw new ConvertException(e.getMessage() + "\n Log gile:" + (logFile != null ? logFile.getAbsolutePath() : "sout"), e);
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

    public FileResult convert(ConversionQueueItem fileQueueItem, SmartTempFile file) {
        File logFile = processExecutor.getErrorLogFile();

        FileResult fileResult;

        try (Lg log = logFile == null ? new SoutLg() : new FileLg(logFile)) {
            LOGGER.debug("Log file({}, {})", fileQueueItem.getId(), logFile == null ? "sout" : logFile.getAbsolutePath());
            log.log("Start pdf 2 word(%s, %s, %s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(), fileQueueItem.getTargetFormat());

            try {
                try {
                    fileResult = doRightConvert(fileQueueItem, file, log);
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                    log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));

                    queueService.exception(fileQueueItem.getId(), e);
                    fileResult = doDirtyConvert(fileQueueItem, file, log);
                }
            } catch (Throwable e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw new ConvertException(e.getMessage() + "\n Log gile:" + (logFile != null ? logFile.getAbsolutePath() : "sout"), e);
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

    private FileResult doRightConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) {
        Document document = new Document(file.getAbsolutePath());
        try {
            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            DocSaveOptions docSaveOptions = new DocSaveOptions();
            docSaveOptions.setFormat(fileQueueItem.getTargetFormat() == Format.DOC ? DocSaveOptions.DocFormat.Doc : DocSaveOptions.DocFormat.DocX);
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
        } finally {
            document.dispose();
        }
    }

    private FileResult doDirtyConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) throws Exception {
        log.log("Start dirty way pdf 2 word(%s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId());

        SmartTempFile tempDir = fileService.createTempDir(fileQueueItem.getUserId(), TAG);
        try {
            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

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
            return new FileResult(fileName, result);
        } finally {
            tempDir.smartDelete();
        }
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

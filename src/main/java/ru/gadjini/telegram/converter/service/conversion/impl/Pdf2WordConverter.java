package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.DocSaveOptions;
import com.aspose.pdf.Document;
import com.aspose.pdf.UnifiedSaveOptions;
import org.apache.commons.io.FileUtils;
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
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.io.IOException;
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

    @Autowired
    public Pdf2WordConverter(TempFileService fileService, FileManager fileManager, PdfValidator fileValidator) {
        super(MAP);
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.fileValidator = fileValidator;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        File logFile = getLogFile();

        FileResult fileResult;

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

                fileResult = doConvert(fileQueueItem, file, log);
            } catch (Throwable e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw new ConvertException(e.getMessage() + "\n Log gile:" + (logFile != null ? logFile.getAbsolutePath() : "sout"), e);
            } finally {
                file.smartDelete();
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

    public FileResult convert(ConversionQueueItem fileQueueItem, SmartTempFile file) {
        File logFile = getLogFile();

        FileResult fileResult;

        try (Lg log = logFile == null ? new SoutLg() : new FileLg(logFile)) {
            LOGGER.debug("Log file({}, {})", fileQueueItem.getId(), logFile == null ? "sout" : logFile.getAbsolutePath());
            log.log("Start pdf 2 word(%s, %s, %s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(), fileQueueItem.getTargetFormat());

            try {
                fileResult = doConvert(fileQueueItem, file, log);
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

    private File getLogFile() {
        try {
            return File.createTempFile(TAG, ".txt");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) {
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
}

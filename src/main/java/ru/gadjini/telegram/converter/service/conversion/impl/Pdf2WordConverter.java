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
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.service.conversion.LocalProcessExecutor;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.logger.FileLg;
import ru.gadjini.telegram.converter.service.logger.Lg;
import ru.gadjini.telegram.converter.service.logger.SoutLg;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.List;
import java.util.Map;

@Component
public class Pdf2WordConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pdf2WordConverter.class);

    public static final String TAG = "pdf2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX));

    private ProcessExecutor processExecutor;

    private ConversionProperties conversionProperties;

    private LocalProcessExecutor localProcessExecutor;

    @Autowired
    public Pdf2WordConverter(ProcessExecutor processExecutor, ConversionProperties conversionProperties,
                             LocalProcessExecutor localProcessExecutor) {
        super(MAP);
        this.processExecutor = processExecutor;
        this.conversionProperties = conversionProperties;
        this.localProcessExecutor = localProcessExecutor;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        File logFile = processExecutor.getErrorLogFile();

        ConversionResult fileResult;

        try (Lg log = logFile == null ? new SoutLg() : new FileLg(logFile)) {
            LOGGER.debug("Log file({}, {})", fileQueueItem.getId(), logFile == null ? "sout" : logFile.getAbsolutePath());
            log.log("Start pdf 2 word(%s, %s, %s, %s)", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(), fileQueueItem.getTargetFormat());

            try {
                fileResult = doRightConvert(fileQueueItem, file, log);
            } catch (ProcessTimedOutException e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw e;
            } catch (Throwable e) {
                log.log("%s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
                throw new ConvertException(e.getMessage() + "\n Log gile:" + (logFile != null ? logFile.getAbsolutePath() : "sout") + "\n" + ExceptionUtils.getStackTrace(e));
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

    private ConversionResult doRightConvert(ConversionQueueItem fileQueueItem, SmartTempFile file, Lg log) {
        Document document = new Document(file.getAbsolutePath());
        try {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                return localProcessExecutor.execute(conversionProperties.getTimeOut(), () -> {
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
                }, document::dispose);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw e;
            }
        } finally {
            document.dispose();
        }
    }
}

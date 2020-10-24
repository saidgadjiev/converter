package ru.gadjini.telegram.converter.job;

import com.aspose.words.License;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ResultType;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposeExecutorService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendSticker;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileWorkObject;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

@Component
public class ConversionJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionJob.class);

    private ConversionQueueService queueService;

    private SmartExecutorService executor;

    private FileManager fileManager;

    private UserService userService;

    private InlineKeyboardService inlineKeyboardService;

    private MediaMessageService mediaMessageService;

    private MessageService messageService;

    private ConversionMessageBuilder messageBuilder;

    private Set<Any2AnyConverter> any2AnyConverters = new LinkedHashSet<>();

    private FileLimitProperties fileLimitProperties;

    private AsposeExecutorService asposeExecutorService;

    @Autowired
    public ConversionJob(ConversionQueueService queueService,
                         FileManager fileManager, UserService userService,
                         InlineKeyboardService inlineKeyboardService, @Qualifier("forceMedia") MediaMessageService mediaMessageService,
                         @Qualifier("messageLimits") MessageService messageService, ConversionMessageBuilder messageBuilder,
                         FileLimitProperties fileLimitProperties, AsposeExecutorService asposeExecutorService) {
        this.queueService = queueService;
        this.fileManager = fileManager;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.mediaMessageService = mediaMessageService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
        this.fileLimitProperties = fileLimitProperties;
        this.asposeExecutorService = asposeExecutorService;
    }

    @Autowired
    public void setAny2AnyConverters(Set<Any2AnyConverter> any2AnyConvertersSet) {
        any2AnyConverters.addAll(any2AnyConvertersSet);
    }

    @Autowired
    public void setExecutor(@Qualifier("conversionTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    @PostConstruct
    public void init() {
        applyAsposeLicenses();
        try {
            queueService.resetProcessing();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        pushJobs();
    }

    @Scheduled(fixedDelay = 5000)
    public void pushJobs() {
        ThreadPoolExecutor heavyExecutor = executor.getExecutor(SmartExecutorService.JobWeight.HEAVY);
        if (heavyExecutor.getActiveCount() < heavyExecutor.getCorePoolSize()) {
            Collection<ConversionQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.HEAVY, heavyExecutor.getCorePoolSize() - heavyExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push heavy jobs({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new ConversionTask(queueItem)));
        }
        ThreadPoolExecutor lightExecutor = executor.getExecutor(SmartExecutorService.JobWeight.LIGHT);
        if (lightExecutor.getActiveCount() < lightExecutor.getCorePoolSize()) {
            Collection<ConversionQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, lightExecutor.getCorePoolSize() - lightExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push light jobs({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new ConversionTask(queueItem)));
        }
        if (heavyExecutor.getActiveCount() < heavyExecutor.getCorePoolSize()) {
            Collection<ConversionQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, heavyExecutor.getCorePoolSize() - heavyExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push light jobs to heavy threads({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new ConversionTask(queueItem), SmartExecutorService.JobWeight.HEAVY));
        }
    }

    public void rejectTask(SmartExecutorService.Job job) {
        queueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({}, {})", job.getId(), job.getWeight());
    }

    public int removeAndCancelCurrentTasks(long chatId) {
        List<ConversionQueueItem> conversionQueueItems = queueService.deleteProcessingOrWaitingByUserId((int) chatId);
        for (ConversionQueueItem conversionQueueItem : conversionQueueItems) {
            if (!executor.cancelAndComplete(conversionQueueItem.getId(), true)) {
                fileManager.fileWorkObject(conversionQueueItem.getUserId(), conversionQueueItem.getSize()).stop();
            }
            asposeExecutorService.cancel(conversionQueueItem.getId());
        }

        return conversionQueueItems.size();
    }

    public boolean cancel(int jobId) {
        ConversionQueueItem item = queueService.delete(jobId);

        if (item == null) {
            return false;
        }
        if (!executor.cancelAndComplete(jobId, true)) {
            fileManager.fileWorkObject(item.getId(), item.getSize()).stop();
        }
        asposeExecutorService.cancel(jobId);

        return item.getStatus() != ConversionQueueItem.Status.COMPLETED;
    }

    public void shutdown() {
        executor.shutdown();
        asposeExecutorService.shutdown();
    }

    private void applyAsposeLicenses() {
        try {
            new License().setLicense("license/license-19.lic");
            LOGGER.debug("Word license applied");

            new com.aspose.pdf.License().setLicense("license/license-19.lic");
            LOGGER.debug("Pdf license applied");

            new com.aspose.imaging.License().setLicense("license/license-19.lic");
            LOGGER.debug("Imaging license applied");

            new com.aspose.slides.License().setLicense("license/license-19.lic");
            LOGGER.debug("Slides license applied");

            new com.aspose.cells.License().setLicense("license/license-19.lic");
            LOGGER.debug("Cells license applied");
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public class ConversionTask implements SmartExecutorService.Job {

        private final ConversionQueueItem fileQueueItem;

        private volatile Supplier<Boolean> checker;

        private volatile boolean canceledByUser;

        private FileWorkObject fileWorkObject;

        private ConversionTask(ConversionQueueItem fileQueueItem) {
            this.fileQueueItem = fileQueueItem;
            this.fileWorkObject = fileManager.fileWorkObject(fileQueueItem.getUserId(), fileQueueItem.getSize());
        }

        @Override
        public Integer getReplyToMessageId() {
            return fileQueueItem.getReplyToMessageId();
        }

        @Override
        public boolean isSuppressUserExceptions() {
            return fileQueueItem.isSuppressUserExceptions();
        }

        @Override
        public void execute() throws Exception {
            try {
                fileWorkObject.start();
                Any2AnyConverter candidate = getCandidate(fileQueueItem);
                if (candidate != null) {
                    String size = MemoryUtils.humanReadableByteCount(fileQueueItem.getSize());
                    LOGGER.debug("Start({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());

                    try {
                        if (StringUtils.isNotBlank(fileQueueItem.getResultFileId())) {
                            mediaMessageService.sendFile(fileQueueItem.getUserId(), fileQueueItem.getResultFileId());
                            queueService.complete(fileQueueItem.getId());
                        } else {
                            try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                                if (convertResult.resultType() == ResultType.BUSY) {
                                    queueService.setWaiting(fileQueueItem.getId());
                                    LOGGER.debug("Busy({}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileFormat(), fileQueueItem.getTargetFormat());
                                } else {
                                    sendResult(fileQueueItem, convertResult);
                                    queueService.complete(fileQueueItem.getId());
                                }
                            }
                        }
                        LOGGER.debug("Finish({}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), size);
                    } catch (CorruptedFileException ex) {
                        queueService.completeWithException(fileQueueItem.getId(), ex.getMessage());

                        throw ex;
                    } catch (Throwable ex) {
                        if (checker == null || !checker.get()) {
                            if (FileManager.isNoneCriticalDownloadingException(ex)) {
                                LOGGER.error("Non critical error " + ex.getMessage());
                                handleNoneCriticalDownloadingException(ex);
                            } else {
                                queueService.exceptionStatus(fileQueueItem.getId(), ex);

                                throw ex;
                            }
                        }
                    }
                } else {
                    queueService.converterNotFound(fileQueueItem.getId());
                    LOGGER.debug("Candidate not found({}, {})", fileQueueItem.getUserId(), fileQueueItem.getFirstFileFormat());
                    throw new ConvertException("Candidate not found src " + fileQueueItem.getFirstFileFormat() + " target " + fileQueueItem.getTargetFormat());
                }
            } finally {
                if (checker == null || !checker.get()) {
                    executor.complete(fileQueueItem.getId());
                    fileWorkObject.stop();
                }
            }
        }

        @Override
        public int getId() {
            return fileQueueItem.getId();
        }

        @Override
        public void cancel() {
            fileManager.cancelDownloading(fileQueueItem.getFirstFileId());
            if (canceledByUser) {
                queueService.delete(fileQueueItem.getId());
                LOGGER.debug("Canceled({}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getFirstFileFormat(),
                        fileQueueItem.getTargetFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), fileQueueItem.getFirstFileId());
            }
            asposeExecutorService.cancel(fileQueueItem.getId());
            executor.complete(fileQueueItem.getId());
            fileWorkObject.stop();
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public Supplier<Boolean> getCancelChecker() {
            return checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileQueueItem.getSize() > fileLimitProperties.getLightFileMaxWeight() ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        @Override
        public long getChatId() {
            return fileQueueItem.getUserId();
        }

        @Override
        public int getProgressMessageId() {
            return fileQueueItem.getProgressMessageId();
        }

        @Override
        public String getErrorCode(Throwable e) {
            if (e instanceof CorruptedFileException || e instanceof ProcessException) {
                return MessagesProperties.MESSAGE_DAMAGED_FILE;
            }

            return MessagesProperties.MESSAGE_CONVERSION_FAILED;
        }

        private Any2AnyConverter getCandidate(ConversionQueueItem fileQueueItem) {
            Format format = getCandidateFormat(fileQueueItem);
            for (Any2AnyConverter any2AnyConverter : any2AnyConverters) {
                if (any2AnyConverter.accept(format, fileQueueItem.getTargetFormat())) {
                    return any2AnyConverter;
                }
            }

            return null;
        }

        private void handleNoneCriticalDownloadingException(Throwable ex) {
            queueService.setWaiting(fileQueueItem.getId(), ex);
            if (StringUtils.isNotBlank(fileQueueItem.getException())
                    && !fileQueueItem.getException().contains(ClassUtils.getShortClassName(ex, null))) {
                updateProgressMessageAfterNoneCriticalException(fileQueueItem.getId());
            }
        }

        private Format getCandidateFormat(ConversionQueueItem queueItem) {
            if (queueItem.getFiles().size() > 1 && queueItem.getFiles().stream().allMatch(m -> m.getFormat().getCategory() == FormatCategory.IMAGES)) {
                return Format.IMAGES;
            }

            return queueItem.getFirstFileFormat();
        }

        private void updateProgressMessageAfterNoneCriticalException(int id) {
            ConversionQueueItem queueItem = queueService.getItem(id);

            if (queueItem == null) {
                return;
            }
            Locale locale = userService.getLocaleOrDefault(queueItem.getId());
            String message = messageBuilder.getConversionProcessingMessage(queueItem, queueItem.getSize(), Collections.emptySet(), ConversionStep.WAITING, Lang.JAVA, locale);

            messageService.editMessage(new EditMessageText((long) queueItem.getUserId(), queueItem.getProgressMessageId(), message)
                    .setNoLogging(true)
                    .setReplyMarkup(inlineKeyboardService.getConversionWaitingKeyboard(queueItem.getId(), locale)));
        }

        private Progress progress(long chatId, ConversionQueueItem queueItem) {
            Progress progress = new Progress();
            progress.setChatId(chatId);

            Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

            progress.setLocale(locale.getLanguage());
            progress.setProgressMessageId(queueItem.getProgressMessageId());
            String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, queueItem.getSize(), Collections.emptySet(), ConversionStep.UPLOADING, Lang.PYTHON, locale);
            progress.setProgressMessage(progressMessage);
            progress.setProgressReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));

            String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, queueItem.getSize(), Collections.emptySet(), ConversionStep.COMPLETED, Lang.PYTHON, locale);
            progress.setAfterProgressCompletionMessage(completionMessage);

            return progress;
        }

        private void sendUploadingProgress(ConversionQueueItem conversionQueueItem, FileResult result, Locale locale) {
            if (!isShowingProgress(result.getFile().length())) {
                String uploadingProgressMessage = messageBuilder.getUploadingProgressMessage(conversionQueueItem, locale);
                try {
                    messageService.editMessage(
                            new EditMessageText(conversionQueueItem.getUserId(), conversionQueueItem.getProgressMessageId(), uploadingProgressMessage)
                                    .setReplyMarkup(inlineKeyboardService.getConversionKeyboard(conversionQueueItem.getId(), locale)));
                } catch (Exception e) {
                    LOGGER.error("Ignore exception\n" + e.getMessage(), e);
                }
            }
        }

        private boolean isShowingProgress(long fileSize) {
            return fileSize > 5 * 1024 * 1024;
        }

        private void sendResult(ConversionQueueItem fileQueueItem, ConvertResult convertResult) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            SendFileResult sendFileResult = null;
            switch (convertResult.resultType()) {
                case FILE: {
                    sendUploadingProgress(fileQueueItem, (FileResult) convertResult, locale);
                    SendDocument sendDocumentContext = new SendDocument((long) fileQueueItem.getUserId(), ((FileResult) convertResult).getFileName(), ((FileResult) convertResult).getFile())
                            .setProgress(progress(fileQueueItem.getUserId(), fileQueueItem))
                            .setCaption(fileQueueItem.getMessage())
                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                            .setReplyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                    try {
                        sendFileResult = mediaMessageService.sendDocument(sendDocumentContext);
                    } catch (TelegramApiRequestException ex) {
                        if (ex.getErrorCode() == 400 && ex.getMessage().contains("reply message not found")) {
                            LOGGER.debug("Reply message not found try send without reply");
                            sendDocumentContext.setReplyToMessageId(null);
                            sendFileResult = mediaMessageService.sendDocument(sendDocumentContext);
                        } else {
                            throw ex;
                        }
                    }
                    break;
                }
                case STICKER: {
                    SendSticker sendFileContext = new SendSticker((long) fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                            .setReplyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                    try {
                        sendFileResult = mediaMessageService.sendSticker(sendFileContext);
                    } catch (TelegramApiRequestException ex) {
                        if (ex.getErrorCode() == 400 && ex.getMessage().contains("reply message not found")) {
                            LOGGER.debug("Reply message not found try send without reply");
                            sendFileContext.setReplyToMessageId(null);
                            sendFileResult = mediaMessageService.sendSticker(sendFileContext);
                        } else {
                            throw ex;
                        }
                    }
                    break;
                }
            }
            if (sendFileResult != null) {
                try {
                    LOGGER.debug("Result({}, {})", fileQueueItem.getId(), sendFileResult.getFileId());
                    queueService.setResultFileId(fileQueueItem.getId(), sendFileResult.getFileId());
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
    }
}

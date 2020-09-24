package ru.gadjini.telegram.converter.job;

import com.aspose.pdf.Document;
import com.aspose.words.License;
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
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendSticker;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
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

    @Autowired
    public ConversionJob(ConversionQueueService queueService,
                         FileManager fileManager, UserService userService,
                         InlineKeyboardService inlineKeyboardService, @Qualifier("mediaLimits") MediaMessageService mediaMessageService,
                         @Qualifier("messageLimits") MessageService messageService, ConversionMessageBuilder messageBuilder) {
        this.queueService = queueService;
        this.fileManager = fileManager;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.mediaMessageService = mediaMessageService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
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
        initFonts();
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

            items.forEach(queueItem -> executor.execute(new ConversionTask(queueItem)));
        }
        ThreadPoolExecutor lightExecutor = executor.getExecutor(SmartExecutorService.JobWeight.LIGHT);
        if (lightExecutor.getActiveCount() < lightExecutor.getCorePoolSize()) {
            Collection<ConversionQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, lightExecutor.getCorePoolSize() - lightExecutor.getActiveCount());

            items.forEach(queueItem -> executor.execute(new ConversionTask(queueItem)));
        }
        if (heavyExecutor.getActiveCount() < heavyExecutor.getCorePoolSize()) {
            Collection<ConversionQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, heavyExecutor.getCorePoolSize() - heavyExecutor.getActiveCount());

            items.forEach(queueItem -> executor.execute(new ConversionTask(queueItem), SmartExecutorService.JobWeight.LIGHT));
        }
    }

    public void rejectTask(SmartExecutorService.Job job) {
        queueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({}, {})", job.getId(), job.getWeight());
    }

    public boolean cancel(int jobId) {
        ConversionQueueItem item = queueService.delete(jobId);

        if (item == null) {
            return false;
        }
        if (!executor.cancelAndComplete(jobId, true)) {
            fileManager.fileWorkObject(item.getId(), item.getSize()).stop();
        }

        return item.getStatus() != ConversionQueueItem.Status.COMPLETED;
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void initFonts() {
        LOGGER.debug("Pdf fonts paths {}", Document.getLocalFontPaths());
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
        public void execute() throws Exception {
            try {
                fileWorkObject.start();
                Any2AnyConverter candidate = getCandidate(fileQueueItem);
                if (candidate != null) {
                    String size = MemoryUtils.humanReadableByteCount(fileQueueItem.getSize());
                    LOGGER.debug("Start({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());

                    try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                        sendResult(fileQueueItem, convertResult);
                        queueService.complete(fileQueueItem.getId());
                        LOGGER.debug("Finish({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());
                    } catch (CorruptedFileException ex) {
                        queueService.completeWithException(fileQueueItem.getId(), ex.getMessage());

                        throw ex;
                    } catch (Exception ex) {
                        if (checker == null || !checker.get()) {
                            queueService.exception(fileQueueItem.getId(), ex);

                            throw ex;
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
            return fileQueueItem.getSize() > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
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
        public String getErrorCode(Exception e) {
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

        private Format getCandidateFormat(ConversionQueueItem queueItem) {
            if (queueItem.getFiles().size() > 1 && queueItem.getFiles().stream().allMatch(m -> m.getFormat().getCategory() == FormatCategory.IMAGES)) {
                return Format.IMAGES;
            }

            return queueItem.getFirstFileFormat();
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
                messageService.editMessage(
                        new EditMessageText(conversionQueueItem.getUserId(), conversionQueueItem.getProgressMessageId(), uploadingProgressMessage)
                                .setReplyMarkup(inlineKeyboardService.getConversionKeyboard(conversionQueueItem.getId(), locale)));
            }
        }

        private boolean isShowingProgress(long fileSize) {
            return fileSize > 5 * 1024 * 1024;
        }

        private void sendResult(ConversionQueueItem fileQueueItem, ConvertResult convertResult) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            switch (convertResult.resultType()) {
                case FILE: {
                    sendUploadingProgress(fileQueueItem, (FileResult) convertResult, locale);
                    SendDocument sendDocumentContext = new SendDocument((long) fileQueueItem.getUserId(), ((FileResult) convertResult).getFileName(), ((FileResult) convertResult).getFile())
                            .setProgress(progress(fileQueueItem.getUserId(), fileQueueItem))
                            .setCaption(fileQueueItem.getMessage())
                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                            .setReplyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                    try {
                        mediaMessageService.sendDocument(sendDocumentContext);
                    } catch (TelegramApiRequestException ex) {
                        if (ex.getErrorCode() == 400 && ex.getMessage().contains("reply message not found")) {
                            LOGGER.debug("Reply message not found try send without reply");
                            sendDocumentContext.setReplyToMessageId(null);
                            mediaMessageService.sendDocument(sendDocumentContext);
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
                        mediaMessageService.sendSticker(sendFileContext);
                    } catch (TelegramApiRequestException ex) {
                        if (ex.getErrorCode() == 400 && ex.getMessage().contains("reply message not found")) {
                            LOGGER.debug("Reply message not found try send without reply");
                            sendFileContext.setReplyToMessageId(null);
                            mediaMessageService.sendSticker(sendFileContext);
                        } else {
                            throw ex;
                        }
                    }
                    break;
                }
            }
        }
    }
}

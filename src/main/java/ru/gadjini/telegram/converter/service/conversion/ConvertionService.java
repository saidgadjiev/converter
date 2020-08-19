package ru.gadjini.telegram.converter.service.conversion;

import com.aspose.pdf.Document;
import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.bot.command.convert.ConvertState;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.converter.model.bot.api.method.send.SendSticker;
import ru.gadjini.telegram.converter.model.bot.api.object.User;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.Format;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.file.FileManager;
import ru.gadjini.telegram.converter.service.file.FileWorkObject;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.message.MediaMessageService;
import ru.gadjini.telegram.converter.service.message.MessageService;
import ru.gadjini.telegram.converter.service.queue.conversion.ConversionQueueService;
import ru.gadjini.telegram.converter.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class ConvertionService {

    private static final String TAG = "cnvs";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertionService.class);

    private Set<Any2AnyConverter<ConvertResult>> any2AnyConverters = new LinkedHashSet<>();

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private ConversionQueueService queueService;

    private LocalisationService localisationService;

    private UserService userService;

    private SmartExecutorService executor;

    private FileManager fileManager;

    @Autowired
    public ConvertionService(@Qualifier("messagelimits") MessageService messageService,
                             LocalisationService localisationService, UserService userService, InlineKeyboardService inlineKeyboardService,
                             @Qualifier("medialimits") MediaMessageService mediaMessageService, ConversionQueueService queueService, FileManager fileManager) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.mediaMessageService = mediaMessageService;
        this.queueService = queueService;
        this.fileManager = fileManager;
    }

    @PostConstruct
    public void init() {
        initFonts();
        applyAsposeLicenses();
        queueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setAny2AnyConverters(Set<Any2AnyConverter> any2AnyConvertersSet) {
        any2AnyConvertersSet.forEach(any2AnyConverters::add);
    }

    @Autowired
    public void setExecutor(@Qualifier("conversionTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectTask(SmartExecutorService.Job job) {
        queueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({})", job.getWeight());
    }

    public ConversionTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            ConversionQueueItem peek = queueService.poll(weight);

            if (peek != null) {
                return new ConversionTask(peek);
            }
            return null;
        }
    }

    public void executeTask(int id) {
        ConversionQueueItem item = queueService.poll(id);
        if (item != null) {
            executor.execute(new ConversionTask(item));
        }
    }

    public ConversionQueueItem convert(User user, ConvertState convertState, Format targetFormat) {
        ConversionQueueItem queueItem = queueService.createProcessingItem(user, convertState, targetFormat);

        fileManager.setInputFilePending(user.getId(), convertState.getMessageId(), convertState.getFileId(), convertState.getFileSize(), TAG);
        executor.execute(new ConversionTask(queueItem));

        return queueItem;
    }

    public void cancel(int jobId) {
        if (!executor.cancelAndComplete(jobId, true)) {
            queueService.delete(jobId);
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<ConversionQueueItem> tasks = queueService.poll(jobWeight, executor.getCorePoolSize(jobWeight));
        for (ConversionQueueItem item : tasks) {
            executor.execute(new ConversionTask(item));
        }
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
        public void run() {
            try {
                fileWorkObject.start();
                Any2AnyConverter<ConvertResult> candidate = getCandidate(fileQueueItem);
                if (candidate != null) {
                    String size = MemoryUtils.humanReadableByteCount(fileQueueItem.getSize());
                    LOGGER.debug("Start({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());

                    try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                        sendResult(fileQueueItem, convertResult);
                        queueService.complete(fileQueueItem.getId());
                        LOGGER.debug("Finish({}, {}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId(), convertResult.time());
                    } catch (CorruptedFileException ex) {
                        queueService.completeWithException(fileQueueItem.getId(), ex.getMessage());
                        LOGGER.error(ex.getMessage());
                        Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                        messageService.sendMessage(
                                new HtmlMessage((long) fileQueueItem.getUserId(), localisationService.getMessage(MessagesProperties.MESSAGE_DAMAGED_FILE, locale))
                                        .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                        );
                    } catch (Exception ex) {
                        if (checker == null || !checker.get()) {
                            queueService.exception(fileQueueItem.getId(), ex);
                            LOGGER.error(ex.getMessage(), ex);
                            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                            messageService.sendMessage(
                                    new HtmlMessage((long) fileQueueItem.getUserId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERSION_FAILED, locale))
                                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                            );
                        }
                    }
                } else {
                    queueService.converterNotFound(fileQueueItem.getId());
                    LOGGER.debug("Candidate not found({}, {})", fileQueueItem.getUserId(), fileQueueItem.getFormat());
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
            fileManager.cancelDownloading(fileQueueItem.getFileId());
            if (canceledByUser) {
                queueService.delete(fileQueueItem.getId());
                LOGGER.debug("Canceled({}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getFormat(),
                        fileQueueItem.getTargetFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), fileQueueItem.getFileId());
            }
            executor.complete(fileQueueItem.getId());
            fileWorkObject.stop();
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileQueueItem.getSize() > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private Any2AnyConverter<ConvertResult> getCandidate(ConversionQueueItem fileQueueItem) {
            for (Any2AnyConverter<ConvertResult> any2AnyConverter : any2AnyConverters) {
                if (any2AnyConverter.accept(fileQueueItem.getFormat(), fileQueueItem.getTargetFormat())) {
                    return any2AnyConverter;
                }
            }

            return null;
        }

        private void sendResult(ConversionQueueItem fileQueueItem, ConvertResult convertResult) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            switch (convertResult.resultType()) {
                case FILE: {
                    SendDocument sendDocumentContext = new SendDocument((long) fileQueueItem.getUserId(), ((FileResult) convertResult).getFileName(), ((FileResult) convertResult).getFile())
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

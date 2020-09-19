package ru.gadjini.telegram.converter.service.conversion;

import com.aspose.pdf.Document;
import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.impl.ConvertState;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendSticker;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.User;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class ConvertionService {

    private static final String TAG = "cnvs";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertionService.class);

    private Set<Any2AnyConverter> any2AnyConverters = new LinkedHashSet<>();

    private InlineKeyboardService inlineKeyboardService;

    private MediaMessageService mediaMessageService;

    private MessageService messageService;

    private ConversionQueueService queueService;

    private LocalisationService localisationService;

    private UserService userService;

    private SmartExecutorService executor;

    private FileManager fileManager;

    private ConversionMessageBuilder messageBuilder;

    private CommandStateService commandStateService;

    private ConverterReplyKeyboardService replyKeyboardService;

    @Autowired
    public ConvertionService(UserService userService, InlineKeyboardService inlineKeyboardService,
                             @Qualifier("mediaLimits") MediaMessageService mediaMessageService, @Qualifier("messageLimits") MessageService messageService,
                             ConversionQueueService queueService, LocalisationService localisationService,
                             FileManager fileManager, ConversionMessageBuilder messageBuilder,
                             CommandStateService commandStateService, @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService) {
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.mediaMessageService = mediaMessageService;
        this.messageService = messageService;
        this.queueService = queueService;
        this.localisationService = localisationService;
        this.fileManager = fileManager;
        this.messageBuilder = messageBuilder;
        this.commandStateService = commandStateService;
        this.replyKeyboardService = replyKeyboardService;
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

    public void convert(User user, ConvertState convertState, Format targetFormat, Locale locale) {
        ConversionQueueItem queueItem = queueService.createProcessingItem(user, convertState, targetFormat);

        sendConversionQueuedMessage(queueItem, convertState, message -> {
            queueItem.setProgressMessageId(message.getMessageId());
            queueService.setProgressMessageId(queueItem.getId(), message.getMessageId());
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                    .setReplyMarkup(replyKeyboardService.removeKeyboard(message.getChatId())));
            commandStateService.deleteState(message.getChatId(), CommandNames.START_COMMAND);

            fileManager.setInputFilePending(user.getId(), convertState.getMessageId(), queueItem.getFirstFileId(), queueItem.getFirstSize(), TAG);
            executor.execute(new ConversionTask(queueItem));
        }, locale);
    }

    public boolean cancel(int jobId) {
        ConversionQueueItem item = queueService.delete(jobId);

        if (item == null) {
            return false;
        }
        if (!executor.cancelAndComplete(jobId, true)) {
            fileManager.fileWorkObject(item.getId(), item.getFirstSize()).stop();
        }

        return item.getStatus() != ConversionQueueItem.Status.COMPLETED;
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

    private void sendConversionQueuedMessage(ConversionQueueItem queueItem, ConvertState convertState, Consumer<Message> callback, Locale locale) {
        String queuedMessage = messageBuilder.getConversionProcessingMessage(queueItem, convertState.getWarnings(), ConversionStep.WAITING, Lang.JAVA, new Locale(convertState.getUserLanguage()));
        messageService.sendMessage(new HtmlMessage((long) queueItem.getUserId(), queuedMessage)
                .setReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale)), callback);
    }

    private Progress progress(long chatId, ConversionQueueItem queueItem) {
        Progress progress = new Progress();
        progress.setChatId(chatId);

        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        progress.setLocale(locale.getLanguage());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.UPLOADING, Lang.PYTHON, locale);
        progress.setProgressMessage(progressMessage);
        progress.setProgressReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));

        String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.COMPLETED, Lang.PYTHON, locale);
        progress.setAfterProgressCompletionMessage(completionMessage);

        return progress;
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
            this.fileWorkObject = fileManager.fileWorkObject(fileQueueItem.getUserId(), fileQueueItem.getFirstSize());
        }

        @Override
        public void execute() throws Exception {
            try {
                fileWorkObject.start();
                Any2AnyConverter candidate = getCandidate(fileQueueItem);
                if (candidate != null) {
                    String size = MemoryUtils.humanReadableByteCount(fileQueueItem.getFirstSize());
                    LOGGER.debug("Start({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());

                    try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                        sendResult(fileQueueItem, convertResult);
                        queueService.complete(fileQueueItem.getId());
                        LOGGER.debug("Finish({}, {}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId(), convertResult.time());
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
                        fileQueueItem.getTargetFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getFirstSize()), fileQueueItem.getFirstFileId());
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
            return fileQueueItem.getFirstSize() > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
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

        private void sendResult(ConversionQueueItem fileQueueItem, ConvertResult convertResult) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            switch (convertResult.resultType()) {
                case FILE: {
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

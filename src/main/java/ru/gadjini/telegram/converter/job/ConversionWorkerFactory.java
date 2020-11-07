package ru.gadjini.telegram.converter.job;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ResultType;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposeExecutorService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.exception.BusyWorkerException;
import ru.gadjini.telegram.smart.bot.commons.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendSticker;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorker;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorkerFactory;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ConversionWorkerFactory implements QueueWorkerFactory<ConversionQueueItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionWorkerFactory.class);

    private FileManager fileManager;

    private UserService userService;

    private InlineKeyboardService inlineKeyboardService;

    private MediaMessageService mediaMessageService;

    private MessageService messageService;

    private ConversionMessageBuilder messageBuilder;

    private Set<Any2AnyConverter> any2AnyConverters = new LinkedHashSet<>();

    private AsposeExecutorService asposeExecutorService;

    private ConversionQueueService conversionQueueService;

    @Autowired
    public ConversionWorkerFactory(FileManager fileManager, UserService userService,
                                   InlineKeyboardService inlineKeyboardService, @Qualifier("forceMedia") MediaMessageService mediaMessageService,
                                   @Qualifier("messageLimits") MessageService messageService, ConversionMessageBuilder messageBuilder,
                                   AsposeExecutorService asposeExecutorService, ConversionQueueService conversionQueueService) {
        this.fileManager = fileManager;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.mediaMessageService = mediaMessageService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
        this.asposeExecutorService = asposeExecutorService;
        this.conversionQueueService = conversionQueueService;
    }

    @Autowired
    public void setAny2AnyConverters(Set<Any2AnyConverter> any2AnyConvertersSet) {
        any2AnyConverters.addAll(any2AnyConvertersSet);
    }

    @Override
    public QueueWorker createWorker(ConversionQueueItem item) {
        return new ConversionWorker(item);
    }

    public class ConversionWorker implements QueueWorker {

        private final ConversionQueueItem fileQueueItem;

        private ConversionWorker(ConversionQueueItem fileQueueItem) {
            this.fileQueueItem = fileQueueItem;
        }

        @Override
        public void execute() throws Exception {
            Any2AnyConverter candidate = getCandidate(fileQueueItem);
            if (candidate != null) {
                String size = MemoryUtils.humanReadableByteCount(fileQueueItem.getSize());
                LOGGER.debug("Start({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());
                if (StringUtils.isNotBlank(fileQueueItem.getResultFileId())) {
                    mediaMessageService.sendFile(fileQueueItem.getUserId(), fileQueueItem.getResultFileId());
                } else {
                    try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                        if (convertResult.resultType() == ResultType.BUSY) {
                            LOGGER.debug("Busy({}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFile().getFileId(), fileQueueItem.getTargetFormat());

                            throw new BusyWorkerException();
                        } else {
                            sendResult(fileQueueItem, convertResult);
                        }
                    }
                }
                LOGGER.debug("Finish({}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), size);
            } else {
                throw new ConvertException("Candidate not found src " + fileQueueItem.getFirstFile().getFormat() + " target " + fileQueueItem.getTargetFormat());
            }
        }

        @Override
        public void cancel() {
            fileManager.cancelDownloading(fileQueueItem.getFirstFile().getFileId());
            asposeExecutorService.cancel(fileQueueItem.getId());
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

            return queueItem.getFirstFile().getFormat();
        }

        private Progress progress(long chatId, ConversionQueueItem queueItem) {
            Progress progress = new Progress();
            progress.setChatId(chatId);

            Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

            progress.setLocale(locale.getLanguage());
            progress.setProgressMessageId(queueItem.getProgressMessageId());
            String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.UPLOADING, locale);
            progress.setProgressMessage(progressMessage);
            progress.setProgressReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));

            String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.COMPLETED, locale);
            progress.setAfterProgressCompletionMessage(completionMessage);

            return progress;
        }

        private void sendUploadingProgress(ConversionQueueItem conversionQueueItem, Locale locale) {
            String uploadingProgressMessage = messageBuilder.getUploadingProgressMessage(conversionQueueItem, locale);
            try {
                messageService.editMessage(
                        new EditMessageText(conversionQueueItem.getUserId(), conversionQueueItem.getProgressMessageId(), uploadingProgressMessage)
                                .setReplyMarkup(inlineKeyboardService.getConversionKeyboard(conversionQueueItem.getId(), locale)));
            } catch (Exception e) {
                LOGGER.error("Ignore exception\n" + e.getMessage(), e);
            }
        }

        private void sendResult(ConversionQueueItem fileQueueItem, ConvertResult convertResult) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            SendFileResult sendFileResult = null;
            switch (convertResult.resultType()) {
                case FILE: {
                    sendUploadingProgress(fileQueueItem, locale);
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
                    conversionQueueService.setResultFileId(fileQueueItem.getId(), sendFileResult.getFileId());
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
    }
}

package ru.gadjini.telegram.converter.job;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.result.*;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposeExecutorService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.exception.BusyWorkerException;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileUploadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.localisation.ErrorCode;
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

    private FileUploadService fileUploadService;

    private UserService userService;

    private SmartInlineKeyboardService smartInlineKeyboardService;

    private InlineKeyboardService inlineKeyboardService;

    private MediaMessageService mediaMessageService;

    private MessageService messageService;

    private ConversionMessageBuilder messageBuilder;

    private Set<Any2AnyConverter> any2AnyConverters = new LinkedHashSet<>();

    private AsposeExecutorService asposeExecutorService;

    private ConversionQueueService conversionQueueService;

    @Autowired
    public ConversionWorkerFactory(UserService userService,
                                   SmartInlineKeyboardService smartInlineKeyboardService, InlineKeyboardService inlineKeyboardService,
                                   @Qualifier("forceMedia") MediaMessageService mediaMessageService,
                                   @Qualifier("messageLimits") MessageService messageService, ConversionMessageBuilder messageBuilder,
                                   AsposeExecutorService asposeExecutorService, ConversionQueueService conversionQueueService) {
        this.userService = userService;
        this.smartInlineKeyboardService = smartInlineKeyboardService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.mediaMessageService = mediaMessageService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
        this.asposeExecutorService = asposeExecutorService;
        this.conversionQueueService = conversionQueueService;
    }

    @Autowired
    public void setFileUploadService(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Autowired
    public void setAny2AnyConverters(Set<Any2AnyConverter> any2AnyConvertersSet) {
        any2AnyConverters.addAll(any2AnyConvertersSet);
    }

    @Override
    public QueueWorker createWorker(ConversionQueueItem item) {
        return new ConversionWorker(item);
    }

    public Any2AnyConverter getCandidate(ConversionQueueItem fileQueueItem) {
        Format format = getCandidateFormat(fileQueueItem);
        for (Any2AnyConverter any2AnyConverter : any2AnyConverters) {
            if (any2AnyConverter.accept(format, fileQueueItem.getTargetFormat())) {
                return any2AnyConverter;
            }
        }

        return null;
    }

    private Format getCandidateFormat(ConversionQueueItem queueItem) {
        if (queueItem.getFiles().size() > 1) {
            if (queueItem.getFiles().stream().allMatch(m -> m.getFormat().getCategory() == FormatCategory.IMAGES)) {
                return Format.IMAGES;
            } else if (queueItem.getFiles().stream().allMatch(m -> m.getFormat() == Format.PDF)) {
                return Format.PDFS;
            }
        }

        return queueItem.getFirstFile().getFormat();
    }

    public class ConversionWorker implements QueueWorker {

        private final ConversionQueueItem fileQueueItem;

        private ConversionWorker(ConversionQueueItem fileQueueItem) {
            this.fileQueueItem = fileQueueItem;
        }

        @Override
        public void execute() {
            Any2AnyConverter candidate = getCandidate(fileQueueItem);
            if (candidate != null) {
                String size = MemoryUtils.humanReadableByteCount(fileQueueItem.getSize());
                LOGGER.debug("Start({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());
                if (StringUtils.isNotBlank(fileQueueItem.getResultFileId())) {
                    mediaMessageService.sendDocument(new SendDocument(String.valueOf(fileQueueItem.getUserId()), new InputFile(fileQueueItem.getResultFileId())));
                } else {
                    sendConvertingProgress(fileQueueItem);
                    ConvertResult convertResult = candidate.convert(fileQueueItem);
                    if (convertResult.resultType() == ResultType.BUSY) {
                        LOGGER.debug("Busy({}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFile().getFileId(), fileQueueItem.getTargetFormat());

                        sendWaitingProgress(fileQueueItem);
                        throw new BusyWorkerException();
                    } else {
                        sendConvertingFinishedProgress(fileQueueItem);
                        sendResult(fileQueueItem, convertResult);
                    }
                }
                LOGGER.debug("Finish({}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), size);
            } else {
                throw new ConvertException("Candidate not found src " + fileQueueItem.getFirstFile().getFormat() + " target " + fileQueueItem.getTargetFormat());
            }
        }

        @Override
        public void cancel(boolean canceledByUser) {
            asposeExecutorService.cancel(fileQueueItem.getId());
        }

        @Override
        public ErrorCode getErrorCode(Throwable e) {
            if (e instanceof CorruptedFileException || e instanceof ProcessException) {
                return new ErrorCode(MessagesProperties.MESSAGE_DAMAGED_FILE);
            }

            return new ErrorCode(MessagesProperties.MESSAGE_CONVERSION_FAILED);
        }

        private Progress progress(long chatId, ConversionQueueItem queueItem) {
            Progress progress = new Progress();
            progress.setChatId(chatId);

            Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

            progress.setProgressMessageId(queueItem.getProgressMessageId());
            String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.UPLOADING, Collections.emptySet(), locale);
            progress.setProgressMessage(progressMessage);
            progress.setProgressReplyMarkup(smartInlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

            String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.COMPLETED, Collections.emptySet(), locale);
            progress.setAfterProgressCompletionMessage(completionMessage);

            return progress;
        }

        private void sendWaitingProgress(ConversionQueueItem queueItem) {
            Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
            String queuedMessage = messageBuilder.getConversionProcessingMessage(queueItem,
                    Collections.emptySet(), ConversionStep.WAITING, Set.of(ConversionStep.DOWNLOADING), locale);

            try {
                messageService.editMessage(EditMessageText.builder().chatId(String.valueOf(queueItem.getUserId()))
                        .messageId(queueItem.getProgressMessageId()).text(queuedMessage)
                        .replyMarkup(smartInlineKeyboardService.getWaitingKeyboard(queueItem.getId(), locale))
                        .build());
            } catch (Exception e) {
                LOGGER.error("Ignore exception\n" + e.getMessage(), e);
            }
        }

        private void sendConvertingProgress(ConversionQueueItem queueItem) {
            Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
            String message = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(),
                    ConversionStep.CONVERTING, Collections.emptySet(), locale);

            try {
                messageService.editMessage(
                        EditMessageText.builder().chatId(String.valueOf(queueItem.getUserId()))
                                .messageId(queueItem.getProgressMessageId()).text(message)
                                .replyMarkup(smartInlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale))
                                .build());
            } catch (Exception e) {
                LOGGER.error("Ignore exception\n" + e.getMessage(), e);
            }
        }

        private void sendConvertingFinishedProgress(ConversionQueueItem conversionQueueItem) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            String uploadingProgressMessage = messageBuilder.getConversionProcessingMessage(conversionQueueItem, Collections.emptySet(),
                    ConversionStep.WAITING, Set.of(ConversionStep.DOWNLOADING, ConversionStep.CONVERTING), locale);
            try {
                messageService.editMessage(
                        EditMessageText.builder().chatId(String.valueOf(conversionQueueItem.getUserId()))
                                .messageId(conversionQueueItem.getProgressMessageId()).text(uploadingProgressMessage)
                                .replyMarkup(smartInlineKeyboardService.getWaitingKeyboard(conversionQueueItem.getId(), locale))
                                .build());
            } catch (Exception e) {
                LOGGER.error("Ignore exception\n" + e.getMessage(), e);
            }
        }

        private void sendResult(ConversionQueueItem fileQueueItem, ConvertResult convertResult) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            SendFileResult sendFileResult = null;
            switch (convertResult.resultType()) {
                case FILE: {
                    FileResult fileResult = (FileResult) convertResult;
                    SendDocument.SendDocumentBuilder sendDocumentBuilder = SendDocument.builder().chatId(String.valueOf(fileQueueItem.getUserId()))
                            .document(new InputFile(fileResult.getFile(), fileResult.getFileName()))
                            .caption(fileResult.getCaption())
                            .replyToMessageId(fileQueueItem.getReplyToMessageId())
                            .replyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                    if (fileResult.getThumb() != null) {
                        sendDocumentBuilder.thumb(new InputFile(fileResult.getThumb(), fileResult.getThumb().getName()));
                    }
                    fileUploadService.createUpload(fileQueueItem.getUserId(), SendDocument.PATH, sendDocumentBuilder.build(),
                            progress(fileQueueItem.getUserId(), fileQueueItem), fileQueueItem.getId());

                    break;
                }
                case STICKER: {
                    SendSticker sendFileContext = SendSticker.builder().chatId(String.valueOf(fileQueueItem.getUserId()))
                            .sticker(new InputFile(((FileResult) convertResult).getFile(), ((FileResult) convertResult).getFileName()))
                            .replyToMessageId(fileQueueItem.getReplyToMessageId())
                            .replyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale))
                            .build();
                    sendFileResult = mediaMessageService.sendSticker(sendFileContext);

                    break;
                }
                case AUDIO: {
                    AudioResult audioResult = (AudioResult) convertResult;
                    SendAudio.SendAudioBuilder sendAudioBuilder = SendAudio.builder().chatId(String.valueOf(fileQueueItem.getUserId()))
                            .audio(new InputFile(audioResult.getFile(), audioResult.getFileName()))
                            .replyToMessageId(fileQueueItem.getReplyToMessageId())
                            .performer(audioResult.getAudioPerformer())
                            .caption(audioResult.getCaption())
                            .title(audioResult.getAudioTitle());
                    if (audioResult.getThumb() != null) {
                        sendAudioBuilder.thumb(new InputFile(audioResult.getThumb(), audioResult.getThumb().getName()));
                    }
                    if (audioResult.getDuration() != null) {
                        sendAudioBuilder.duration(audioResult.getDuration());
                    }
                    SendAudio sendAudio = sendAudioBuilder.replyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale))
                            .build();
                    fileUploadService.createUpload(fileQueueItem.getUserId(), SendAudio.PATH, sendAudio,
                            progress(fileQueueItem.getUserId(), fileQueueItem), fileQueueItem.getId());
                    break;
                }
                case CONTAINER: {
                    ConvertResults convertResults = (ConvertResults) convertResult;
                    for (ConvertResult result : convertResults.getConvertResults()) {
                        sendResult(fileQueueItem, result);
                    }
                    break;
                }
                case VOICE:
                    VoiceResult voiceResult = (VoiceResult) convertResult;
                    SendVoice.SendVoiceBuilder sendVoiceBuilder = SendVoice.builder().chatId(String.valueOf(fileQueueItem.getUserId()))
                            .voice(new InputFile(voiceResult.getFile(), voiceResult.getFileName()))
                            .caption(voiceResult.getCaption())
                            .replyToMessageId(fileQueueItem.getReplyToMessageId());
                    if (voiceResult.getDuration() != null) {
                        sendVoiceBuilder.duration(voiceResult.getDuration());
                    }
                    SendVoice sendVoice = sendVoiceBuilder.replyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale))
                            .build();
                    fileUploadService.createUpload(fileQueueItem.getUserId(), SendVoice.PATH, sendVoice,
                            progress(fileQueueItem.getUserId(), fileQueueItem), fileQueueItem.getId());
                    break;
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

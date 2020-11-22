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
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.result.*;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposeExecutorService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.exception.BusyWorkerException;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
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

    private FileDownloadService fileDownloadService;

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
    public ConversionWorkerFactory(FileDownloadService fileDownloadService, UserService userService,
                                   SmartInlineKeyboardService smartInlineKeyboardService, InlineKeyboardService inlineKeyboardService, @Qualifier("forceMedia") MediaMessageService mediaMessageService,
                                   @Qualifier("messageLimits") MessageService messageService, ConversionMessageBuilder messageBuilder,
                                   AsposeExecutorService asposeExecutorService, ConversionQueueService conversionQueueService) {
        this.fileDownloadService = fileDownloadService;
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
        if (queueItem.getFiles().size() > 1 && queueItem.getFiles().stream().allMatch(m -> m.getFormat().getCategory() == FormatCategory.IMAGES)) {
            return Format.IMAGES;
        }

        return queueItem.getFirstFile().getFormat();
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
                    mediaMessageService.sendDocument(new SendDocument(String.valueOf(fileQueueItem.getUserId()), new InputFile(fileQueueItem.getResultFileId())));
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
            fileDownloadService.cancelDownload(fileQueueItem.getFirstFile().getFileId(), fileQueueItem.getSize());
            asposeExecutorService.cancel(fileQueueItem.getId());
        }

        private Progress progress(long chatId, ConversionQueueItem queueItem) {
            Progress progress = new Progress();
            progress.setChatId(chatId);

            Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

            progress.setProgressMessageId(queueItem.getProgressMessageId());
            String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.UPLOADING, locale);
            progress.setProgressMessage(progressMessage);
            progress.setProgressReplyMarkup(smartInlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

            String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.COMPLETED, locale);
            progress.setAfterProgressCompletionMessage(completionMessage);

            return progress;
        }

        private void sendUploadingProgress(ConversionQueueItem conversionQueueItem, Locale locale) {
            String uploadingProgressMessage = messageBuilder.getUploadingProgressMessage(conversionQueueItem, locale);
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
                    sendUploadingProgress(fileQueueItem, locale);
                    SendDocument.SendDocumentBuilder sendDocumentBuilder = SendDocument.builder().chatId(String.valueOf(fileQueueItem.getUserId()))
                            .document(new InputFile(fileResult.getFile(), fileResult.getFileName()))
                            .caption(fileResult.getCaption())
                            .replyToMessageId(fileQueueItem.getReplyToMessageId())
                            .replyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                    if (fileResult.getThumb() != null) {
                        sendDocumentBuilder.thumb(new InputFile(fileResult.getThumb(), fileResult.getThumb().getName()));
                    }
                    sendFileResult = mediaMessageService.sendDocument(sendDocumentBuilder.build(), progress(fileQueueItem.getUserId(), fileQueueItem));

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
                    sendFileResult = mediaMessageService.sendAudio(sendAudio, progress(fileQueueItem.getUserId(), fileQueueItem));
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
                    sendFileResult = mediaMessageService.sendVoice(sendVoice, progress(fileQueueItem.getUserId(), fileQueueItem));
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
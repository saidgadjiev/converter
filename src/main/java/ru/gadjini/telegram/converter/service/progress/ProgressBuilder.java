package ru.gadjini.telegram.converter.service.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

@Component
public class ProgressBuilder {

    private UserService userService;

    private ConversionMessageBuilder messageBuilder;

    private SmartInlineKeyboardService inlineKeyboardService;

    @Autowired
    public ProgressBuilder(UserService userService, ConversionMessageBuilder messageBuilder, SmartInlineKeyboardService inlineKeyboardService) {
        this.userService = userService;
        this.messageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    public Progress buildFilesDownloadProgress(ConversionQueueItem queueItem, int current, int total) {
        Progress progress = new Progress();
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());

        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        progress.setProgressMessage(messageBuilder.getFilesDownloadingProgressMessage(queueItem, current, total, locale));
        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        if (current + 1 < total) {
            String completionMessage = messageBuilder.getFilesDownloadingProgressMessage(queueItem, current + 1, total, locale);
            progress.setAfterProgressCompletionMessage(completionMessage);
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));
        } else {
            String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(),
                    ConversionStep.WAITING, Set.of(ConversionStep.DOWNLOADING), locale);
            progress.setAfterProgressCompletionMessage(completionMessage);
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getWaitingKeyboard(queueItem.getId(), locale));
        }

        return progress;
    }

    public Progress buildFileDownloadProgress(ConversionQueueItem queueItem) {
        Progress progress = new Progress();
        progress.setChatId(queueItem.getUserId());

        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        progress.setProgressMessageId(queueItem.getProgressMessageId());
        String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.DOWNLOADING, Collections.emptySet(), locale);
        progress.setProgressMessage(progressMessage);
        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(),
                ConversionStep.WAITING, Set.of(ConversionStep.DOWNLOADING), locale);
        progress.setAfterProgressCompletionMessage(completionMessage);
        progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getWaitingKeyboard(queueItem.getId(), locale));

        return progress;
    }
}

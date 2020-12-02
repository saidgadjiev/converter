package ru.gadjini.telegram.converter.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueJobConfigurator;

import java.util.Collections;
import java.util.Locale;

@Component
public class ConversionQueueJobConfigurator implements QueueJobConfigurator<ConversionQueueItem> {

    private ConversionMessageBuilder messageBuilder;

    private SmartInlineKeyboardService inlineKeyboardService;

    @Autowired
    public ConversionQueueJobConfigurator(ConversionMessageBuilder messageBuilder, SmartInlineKeyboardService inlineKeyboardService) {
        this.messageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public String getWaitingMessage(ConversionQueueItem queueItem, Locale locale) {
        return messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.WAITING, Collections.emptySet(), locale);
    }

    @Override
    public InlineKeyboardMarkup getWaitingKeyboard(ConversionQueueItem queueItem, Locale locale) {
        return inlineKeyboardService.getWaitingKeyboard(queueItem.getId(), locale);
    }

    @Override
    public String getErrorCode(Throwable e) {
        if (e instanceof CorruptedFileException || e instanceof ProcessException) {
            return MessagesProperties.MESSAGE_DAMAGED_FILE;
        }

        return MessagesProperties.MESSAGE_CONVERSION_FAILED;
    }
}

package ru.gadjini.telegram.converter.service.conversion.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.annotation.TelegramMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.property.ProgressProperties;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.Set;

@Component
public class FFmpegProgressCallbackHandlerFactory {

    private ConversionMessageBuilder messageBuilder;

    private MessageService messageService;

    private ProgressProperties progressProperties;

    private SmartInlineKeyboardService smartInlineKeyboardService;

    @Autowired
    protected FFmpegProgressCallbackHandlerFactory(ConversionMessageBuilder messageBuilder,
                                                   @TelegramMessageLimitsControl MessageService messageService,
                                                   ProgressProperties progressProperties,
                                                   SmartInlineKeyboardService smartInlineKeyboardService) {
        this.messageBuilder = messageBuilder;
        this.messageService = messageService;
        this.progressProperties = progressProperties;
        this.smartInlineKeyboardService = smartInlineKeyboardService;
    }

    public FFmpegProgressCallbackHandler createCallback(ConversionQueueItem queueItem, Long duration, Locale locale) {
        return new FFmpegProgressCallbackHandler(duration, progressProperties.getTimeToUpdate()) {
            @Override
            void updateProgressMessage(String eta, String speed, int percentage) {
                String conversionProcessingMessage = messageBuilder.getConversionProcessingMessage(
                        queueItem, ConversionStep.CONVERTING, Set.of(ConversionStep.DOWNLOADING), true, true,
                        locale
                );

                messageService.editMessage(
                        EditMessageText.builder()
                                .messageId(queueItem.getProgressMessageId())
                                .chatId(String.valueOf(queueItem.getUserId()))
                                .text(String.format(conversionProcessingMessage, percentage, eta, speed))
                                .replyMarkup(smartInlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale))
                                .parseMode(ParseMode.HTML)
                                .build()
                );
            }
        };
    }

}

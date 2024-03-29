package ru.gadjini.telegram.converter.service.conversion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.event.ConversionCreatedEvent;
import ru.gadjini.telegram.converter.job.ConversionWorkerFactory;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ConvertionService {

    private SmartInlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private ConversionQueueService conversionQueueService;

    private ConversionMessageBuilder messageBuilder;

    private ConversionWorkerFactory conversionWorkerFactory;

    @Autowired
    public ConvertionService(SmartInlineKeyboardService inlineKeyboardService,
                             @TgMessageLimitsControl MessageService messageService,
                             ConversionQueueService conversionQueueService,
                             ConversionMessageBuilder messageBuilder) {
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageService = messageService;
        this.conversionQueueService = conversionQueueService;
        this.messageBuilder = messageBuilder;
    }

    @Autowired
    public void setConversionWorkerFactory(ConversionWorkerFactory conversionWorkerFactory) {
        this.conversionWorkerFactory = conversionWorkerFactory;
    }

    @Transactional
    public void createConversion(User user, ConvertState convertState, Format targetFormat, Locale locale) {
        ConversionQueueItem queueItem = conversionQueueService.create(user, convertState, targetFormat);

        Any2AnyConverter candidate = conversionWorkerFactory.getCandidate(queueItem);
        AtomicInteger progressMessageId = new AtomicInteger();
        if (candidate.needToSendProgressMessage(queueItem, progressMessageId)) {
            sendConversionQueuedMessage(queueItem, convertState, locale);
        } else {
            queueItem.setProgressMessageId(progressMessageId.get());
            int totalFilesToDownload = candidate.createDownloads(queueItem);
            conversionQueueService.setProgressMessageId(queueItem.getId(), totalFilesToDownload);
        }
    }

    private void sendConversionQueuedMessage(ConversionQueueItem queueItem, ConvertState convertState, Locale locale) {
        String queuedMessage = messageBuilder.getConversionProcessingMessage(queueItem,
                ConversionStep.WAITING, Collections.emptySet(), new Locale(convertState.getUserLanguage()));
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(queueItem.getUserId())).text(queuedMessage)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(inlineKeyboardService.getWaitingKeyboard(queueItem.getId(), locale)).build(),
                new ConversionCreatedEvent(queueItem.getId()));
    }
}

package ru.gadjini.telegram.converter.service.conversion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.job.ConversionWorkerFactory;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.impl.ConvertState;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
public class ConvertionService {

    private SmartInlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private ConversionQueueService conversionQueueService;

    private QueueService queueService;

    private ConversionMessageBuilder messageBuilder;

    private CommandStateService commandStateService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private ConversionWorkerFactory conversionWorkerFactory;

    @Autowired
    public ConvertionService(SmartInlineKeyboardService inlineKeyboardService,
                             @Qualifier("messageLimits") MessageService messageService,
                             ConversionQueueService conversionQueueService, QueueService queueService,
                             ConversionMessageBuilder messageBuilder,
                             CommandStateService commandStateService, @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService,
                             ConversionWorkerFactory conversionWorkerFactory) {
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageService = messageService;
        this.conversionQueueService = conversionQueueService;
        this.queueService = queueService;
        this.messageBuilder = messageBuilder;
        this.commandStateService = commandStateService;
        this.replyKeyboardService = replyKeyboardService;
        this.conversionWorkerFactory = conversionWorkerFactory;
    }

    public void createConversion(User user, ConvertState convertState, Format targetFormat, Locale locale) {
        ConversionQueueItem queueItem = conversionQueueService.create(user, convertState, targetFormat);
        Any2AnyConverter candidate = conversionWorkerFactory.getCandidate(queueItem);

        if (candidate != null) {
            candidate.createDownloads(queueItem);
        }

        sendConversionQueuedMessage(queueItem, convertState, message -> {
            queueItem.setProgressMessageId(message.getMessageId());
            queueService.setProgressMessageId(queueItem.getId(), message.getMessageId());
            messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                    .text(messageBuilder.getWelcomeMessage(locale))
                    .replyMarkup(replyKeyboardService.removeKeyboard(message.getChatId())).build());
            commandStateService.deleteState(message.getChatId(), ConverterCommandNames.START_COMMAND);
        }, locale);
    }

    private void sendConversionQueuedMessage(ConversionQueueItem queueItem, ConvertState convertState, Consumer<Message> callback, Locale locale) {
        String queuedMessage = messageBuilder.getConversionProcessingMessage(queueItem,
                convertState.getWarnings(), ConversionStep.WAITING, new Locale(convertState.getUserLanguage()));
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(queueItem.getUserId())).text(queuedMessage)
                .parseMode(ParseMode.HTML)
                .replyMarkup(inlineKeyboardService.getWaitingKeyboard(queueItem.getId(), locale)).build(), callback);
    }
}

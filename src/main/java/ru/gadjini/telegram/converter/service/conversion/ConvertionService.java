package ru.gadjini.telegram.converter.service.conversion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.impl.ConvertState;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.User;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
public class ConvertionService {

    private static final String TAG = "cnvs";

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private ConversionQueueService queueService;

    private LocalisationService localisationService;

    private FileManager fileManager;

    private ConversionMessageBuilder messageBuilder;

    private CommandStateService commandStateService;

    private ConverterReplyKeyboardService replyKeyboardService;

    @Autowired
    public ConvertionService(InlineKeyboardService inlineKeyboardService,
                             @Qualifier("messageLimits") MessageService messageService,
                             ConversionQueueService queueService, LocalisationService localisationService,
                             FileManager fileManager, ConversionMessageBuilder messageBuilder,
                             CommandStateService commandStateService, @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService) {
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageService = messageService;
        this.queueService = queueService;
        this.localisationService = localisationService;
        this.fileManager = fileManager;
        this.messageBuilder = messageBuilder;
        this.commandStateService = commandStateService;
        this.replyKeyboardService = replyKeyboardService;
    }

    public void createConversion(User user, ConvertState convertState, int canceledTasksCount, Format targetFormat, Locale locale) {
        ConversionQueueItem queueItem = queueService.create(user, convertState, targetFormat);

        sendConversionQueuedMessage(queueItem, convertState, canceledTasksCount, message -> {
            queueItem.setProgressMessageId(message.getMessageId());
            queueService.setProgressMessageId(queueItem.getId(), message.getMessageId());
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                    .setReplyMarkup(replyKeyboardService.removeKeyboard(message.getChatId())));
            commandStateService.deleteState(message.getChatId(), ConverterCommandNames.START_COMMAND);

            fileManager.setInputFilePending(user.getId(), convertState.getMessageId(), queueItem.getFirstFileId(), queueItem.getSize(), TAG);
        }, locale);
    }

    public ConversionQueueItem retryFloodWait(int id) {
        queueService.setSuppressUserExceptions(id, false);
        queueService.setWaiting(id);

        return queueService.getItem(id);
    }

    private void sendConversionQueuedMessage(ConversionQueueItem queueItem, ConvertState convertState, int canceledTasksCount, Consumer<Message> callback, Locale locale) {
        String queuedMessage = messageBuilder.getConversionProcessingMessage(queueItem, queueItem.getSize(), convertState.getWarnings(), ConversionStep.WAITING, Lang.JAVA, new Locale(convertState.getUserLanguage()));
        if (canceledTasksCount > 0) {
            queuedMessage += "\n\n" + localisationService.getMessage(MessagesProperties.MESSAGE_CURRENT_TASKS_CANCELED, locale);
        }
        messageService.sendMessage(new HtmlMessage((long) queueItem.getUserId(), queuedMessage)
                .setReplyMarkup(inlineKeyboardService.getConversionWaitingKeyboard(queueItem.getId(), locale)), callback);
    }
}

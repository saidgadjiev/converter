package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.request.Arg;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.converter.utils.TextUtils;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

@Component
public class UpdateQueryStatusCommand implements CallbackBotCommand {

    private ConversionMessageBuilder messageBuilder;

    private QueueService queueService;

    private UserService userService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    @Autowired
    public UpdateQueryStatusCommand(ConversionMessageBuilder messageBuilder,
                                    QueueService queueService, UserService userService,
                                    @Qualifier("messageLimits") MessageService messageService,
                                    InlineKeyboardService inlineKeyboardService, LocalisationService localisationService) {
        this.messageBuilder = messageBuilder;
        this.queueService = queueService;
        this.userService = userService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localisationService = localisationService;
    }

    @Override
    public String getName() {
        return ConverterCommandNames.UPDATE_QUERY_STATUS;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());
        ConversionQueueItem queueItem = (ConversionQueueItem) queueService.getById(queryItemId);
        if (queueItem == null) {
            messageService.editMessage(
                    new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(),
                            localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, locale))
            );
            messageService.sendAnswerCallbackQuery(
                    new AnswerCallbackQuery(callbackQuery.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, locale))
                            .setShowAlert(true));
        } else {
            String queuedMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.WAITING, locale);
            if (!Objects.equals(TextUtils.removeHtmlTags(queuedMessage), callbackQuery.getMessage().getText())) {
                messageService.editMessage(
                        new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), queuedMessage)
                                .setReplyMarkup(inlineKeyboardService.getConversionWaitingKeyboard(queryItemId, locale))
                );
            }
            messageService.sendAnswerCallbackQuery(
                    new AnswerCallbackQuery(callbackQuery.getId(), localisationService.getMessage(MessagesProperties.UPDATED_CALLBACK_ANSWER, locale)));
        }
    }
}

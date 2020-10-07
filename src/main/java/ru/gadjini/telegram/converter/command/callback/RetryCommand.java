package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.request.Arg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.converter.utils.TextUtils;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

@Component
@SuppressWarnings("CPD-START")
public class RetryCommand implements CallbackBotCommand {

    private ConvertionService convertionService;

    private UserService userService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private ConversionMessageBuilder messageBuilder;

    @Autowired
    public RetryCommand(ConvertionService convertionService, UserService userService,
                        LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                        @Qualifier("messageLimits") MessageService messageService, ConversionMessageBuilder messageBuilder) {
        this.convertionService = convertionService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
    }

    @Override
    public String getName() {
        return CommandNames.RETRY;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());
        ConversionQueueItem queueItem = convertionService.retryFloodWait(queryItemId);
        if (queueItem == null) {
            messageService.deleteMessage(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
            messageService.sendAnswerCallbackQuery(
                    new AnswerCallbackQuery(callbackQuery.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, locale))
                            .setShowAlert(true));
        } else {
            String queuedMessage = messageBuilder.getConversionProcessingMessage(queueItem, queueItem.getSize(), Collections.emptySet(), ConversionStep.WAITING, Lang.JAVA, locale);
            if (!Objects.equals(TextUtils.removeHtmlTags(queuedMessage), callbackQuery.getMessage().getText())) {
                messageService.editMessage(
                        new EditMessageText(callbackQuery.getMessage().getChatId(), queueItem.getProgressMessageId(), queuedMessage)
                                .setReplyMarkup(inlineKeyboardService.getConversionWaitingKeyboard(queryItemId, locale))
                );
            }
            messageService.sendAnswerCallbackQuery(
                    new AnswerCallbackQuery(callbackQuery.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_RETRY_ANSWER, locale)));

            messageService.deleteMessage(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
        }
    }
}

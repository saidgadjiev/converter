package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.converter.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class CancelQueryCommand implements CallbackBotCommand {

    private ConvertionService convertionService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CancelQueryCommand(ConvertionService convertionService, @Qualifier("messageLimits") MessageService messageService,
                              LocalisationService localisationService, UserService userService) {
        this.convertionService = convertionService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_QUERY_COMMAND_NAME;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        boolean cancel = convertionService.cancel(queryItemId);
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());

        messageService.editMessage(
                new EditMessageText(callbackQuery.getMessage().getChatId(),
                        callbackQuery.getMessage().getMessageId(),
                        localisationService.getMessage(cancel ? MessagesProperties.MESSAGE_QUERY_CANCELED : MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, locale)));
        if (!cancel) {
            messageService.sendAnswerCallbackQuery(
                    new AnswerCallbackQuery(callbackQuery.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, locale))
                            .setShowAlert(true));
        }
    }
}

package ru.gadjini.telegram.converter.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.bot.command.api.CallbackBotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.converter.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.converter.request.Arg;
import ru.gadjini.telegram.converter.request.RequestParams;
import ru.gadjini.telegram.converter.service.KeyboardCustomizer;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.message.MessageService;

import java.util.Locale;

@Component
public class CancelQueryCommand implements CallbackBotCommand {

    private ConvertionService convertionService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CancelQueryCommand(ConvertionService convertionService, @Qualifier("messagelimits") MessageService messageService,
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
        convertionService.cancel(queryItemId);
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());

        String actionFrom = requestParams.getString(Arg.ACTION_FROM.getKey());
        if (actionFrom.equals(CommandNames.QUERY_ITEM_DETAILS_COMMAND)) {
            messageService.editMessage(
                    new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, locale))
                            .setReplyMarkup(new KeyboardCustomizer(callbackQuery.getMessage().getReplyMarkup()).removeExclude(CommandNames.GO_BACK_CALLBACK_COMMAND_NAME).getKeyboardMarkup())
            );
        } else {
            messageService.editMessage(
                    new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, locale)));
        }
    }
}

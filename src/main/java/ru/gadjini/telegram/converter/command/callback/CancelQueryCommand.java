package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.job.QueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Component
public class CancelQueryCommand implements CallbackBotCommand {

    private QueueJob conversionJob;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CancelQueryCommand(QueueJob conversionJob, @Qualifier("messageLimits") MessageService messageService,
                              LocalisationService localisationService, UserService userService) {
        this.conversionJob = conversionJob;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return ConverterCommandNames.CANCEL_QUERY_COMMAND_NAME;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        boolean cancel = conversionJob.cancel(queryItemId);
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

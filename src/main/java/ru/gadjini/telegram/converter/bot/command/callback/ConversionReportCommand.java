package ru.gadjini.telegram.converter.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.bot.command.api.CallbackBotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.request.Arg;
import ru.gadjini.telegram.converter.request.RequestParams;
import ru.gadjini.telegram.converter.service.ConversinoReportService;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.message.MessageService;

import java.util.Locale;

@Component
public class ConversionReportCommand implements CallbackBotCommand {

    private ConversinoReportService fileReportService;

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public ConversionReportCommand(ConversinoReportService fileReportService, @Qualifier("messagelimits") MessageService messageService,
                                   UserService userService, LocalisationService localisationService) {
        this.fileReportService = fileReportService;
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
    }

    @Override
    public String getName() {
        return CommandNames.REPORT_COMMAND_NAME;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int itemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());

        fileReportService.createReport(callbackQuery.getFrom().getId(), itemId);

        messageService.removeInlineKeyboard(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());
        messageService.sendMessage(
                new HtmlMessage(callbackQuery.getMessage().getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_REPLY, locale))
        );
    }
}

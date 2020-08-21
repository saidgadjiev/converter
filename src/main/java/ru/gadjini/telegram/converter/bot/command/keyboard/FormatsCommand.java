package ru.gadjini.telegram.converter.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.bot.command.api.BotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.message.MessageService;

@SuppressWarnings("CPD-START")
@Component
public class FormatsCommand implements BotCommand {

    private final MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public FormatsCommand(@Qualifier("messagelimits") MessageService messageService, LocalisationService localisationService, UserService userService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        messageService.sendMessage(
                new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_FORMATS, userService.getLocaleOrDefault(message.getFrom().getId()))));
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.FORMATS_COMMAND;
    }
}

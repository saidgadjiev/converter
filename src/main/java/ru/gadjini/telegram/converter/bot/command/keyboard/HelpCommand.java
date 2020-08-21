package ru.gadjini.telegram.converter.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.bot.command.api.BotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.service.CommandMessageBuilder;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.message.MessageService;

import java.util.Locale;

@Component
public class HelpCommand implements BotCommand {

    private final MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private CommandMessageBuilder commandMessageBuilder;

    @Autowired
    public HelpCommand(@Qualifier("messagelimits") MessageService messageService, LocalisationService localisationService,
                       UserService userService, CommandMessageBuilder commandMessageBuilder) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.commandMessageBuilder = commandMessageBuilder;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        sendHelpMessage(message.getFrom().getId(), userService.getLocaleOrDefault(message.getFrom().getId()));
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.HELP_COMMAND;
    }

    private void sendHelpMessage(int userId, Locale locale) {
        messageService.sendMessage(
                new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_HELP,
                        new Object[]{commandMessageBuilder.getCommandsInfo(locale)},
                        locale)));
    }
}

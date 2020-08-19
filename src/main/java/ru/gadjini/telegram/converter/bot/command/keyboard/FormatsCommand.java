package ru.gadjini.telegram.converter.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.bot.command.api.BotCommand;
import ru.gadjini.telegram.converter.bot.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("CPD-START")
@Component
public class FormatsCommand implements KeyboardBotCommand, BotCommand {

    private final MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private Set<String> names = new HashSet<>();

    @Autowired
    public FormatsCommand(@Qualifier("messagelimits") MessageService messageService, LocalisationService localisationService, UserService userService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.FORMATS_COMMAND_NAME, locale));
        }
    }

    @Override
    public void processMessage(Message message) {
        processMessage(message, null);
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.FORMATS_COMMAND;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getFrom().getId(), userService.getLocaleOrDefault(message.getFrom().getId()));

        return false;
    }

    private void processMessage0(int userId, Locale locale) {
        messageService.sendMessage(
                new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_FORMATS, locale)));
    }
}

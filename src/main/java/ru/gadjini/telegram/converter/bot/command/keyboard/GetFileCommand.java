package ru.gadjini.telegram.converter.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.bot.command.api.BotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.message.MediaMessageService;

@Component
public class GetFileCommand implements BotCommand {

    private MediaMessageService mediaMessageService;

    private UserService userService;

    @Autowired
    public GetFileCommand(@Qualifier("medialimits") MediaMessageService messageService, UserService userService) {
        this.mediaMessageService = messageService;
        this.userService = userService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        if (userService.isAdmin(message.getFrom().getId())) {
            mediaMessageService.sendFile(message.getChatId(), params[0]);
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.GET_FILE_COMMAND;
    }
}

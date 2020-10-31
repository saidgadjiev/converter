package ru.gadjini.telegram.converter.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueService;

@Component
public class GetExceptionCommand implements BotCommand {

    private QueueService queueService;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public GetExceptionCommand(QueueService queueService, @Qualifier("messageLimits") MessageService messageService, UserService userService) {
        this.queueService = queueService;
        this.messageService = messageService;
        this.userService = userService;
    }

    @Override
    public boolean accept(Message message) {
        return userService.isAdmin(message.getFrom().getId());
    }

    @Override
    public void processMessage(Message message, String[] params) {
        String exception = queueService.getException(Integer.parseInt(params[0]));
        messageService.sendMessage(new SendMessage(
                message.getChatId(),
                StringUtils.defaultIfBlank(exception, "No stack trace")
        ));
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.GET_EXCEPTION;
    }
}

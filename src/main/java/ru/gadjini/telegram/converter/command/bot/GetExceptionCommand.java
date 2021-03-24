package ru.gadjini.telegram.converter.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.WorkQueueService;

@Component
public class GetExceptionCommand implements BotCommand {

    private WorkQueueService queueService;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public GetExceptionCommand(WorkQueueService queueService, @TgMessageLimitsControl MessageService messageService, UserService userService) {
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
                String.valueOf(message.getChatId()),
                StringUtils.defaultIfBlank(exception, "No stack trace")
        ));
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.GET_EXCEPTION;
    }
}

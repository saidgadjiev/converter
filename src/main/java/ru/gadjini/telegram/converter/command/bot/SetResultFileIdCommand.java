package ru.gadjini.telegram.converter.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;

@Component
public class SetResultFileIdCommand implements BotCommand {

    private ConversionQueueService queueService;

    private UserService userService;

    @Autowired
    public SetResultFileIdCommand(ConversionQueueService queueService, UserService userService) {
        this.queueService = queueService;
        this.userService = userService;
    }

    @Override
    public boolean accept(Message message) {
        return userService.isAdmin(message.getFrom().getId());
    }

    @Override
    public void processMessage(Message message, String[] params) {
        queueService.setResultFileId(Integer.parseInt(params[0]), params[1]);
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.SET_RESULT_FILE_ID;
    }
}

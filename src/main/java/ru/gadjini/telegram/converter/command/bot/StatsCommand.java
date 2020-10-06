package ru.gadjini.telegram.converter.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

@Component
public class StatsCommand implements BotCommand {

    private UserService userService;

    private LocalisationService localisationService;

    private ConversionQueueService queueService;

    private MessageService messageService;

    @Autowired
    public StatsCommand(UserService userService, LocalisationService localisationService,
                        ConversionQueueService queueService, @Qualifier("messageLimits") MessageService messageService) {
        this.userService = userService;
        this.localisationService = localisationService;
        this.queueService = queueService;
        this.messageService = messageService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        if (userService.isAdmin(message.getFrom().getId())) {
            long processing = queueService.count(ConversionQueueItem.Status.PROCESSING);
            long waiting = queueService.count(ConversionQueueItem.Status.WAITING);
            long errors = queueService.count(ConversionQueueItem.Status.EXCEPTION);

            messageService.sendMessage(
                    new HtmlMessage(message.getChatId(), localisationService.getMessage(
                            MessagesProperties.MESSAGE_STATS, new Object[]{processing, waiting, errors},
                            userService.getLocaleOrDefault(message.getFrom().getId())))
            );
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.STATS_COMMAND;
    }
}

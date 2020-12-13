package ru.gadjini.telegram.converter.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
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
            long todayConversions = queueService.getTodayConversionsCount();
            long yesterdayConversions = queueService.getYesterdayConversionsCount();
            long allConversions = queueService.getAllConversionsCount();
            long activity = queueService.getTodayDailyActiveUsersCount();

            messageService.sendMessage(
                    SendMessage.builder().chatId(String.valueOf(message.getChatId())).text(localisationService.getMessage(
                            MessagesProperties.MESSAGE_CUSTOM_STATS, new Object[]{processing, waiting, errors, todayConversions, yesterdayConversions,
                                    allConversions, activity},
                            userService.getLocaleOrDefault(message.getFrom().getId())))
                            .parseMode(ParseMode.HTML).build()
            );
        }
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.STATS_COMMAND;
    }
}

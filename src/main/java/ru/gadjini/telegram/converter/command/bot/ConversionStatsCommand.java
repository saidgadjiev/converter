package ru.gadjini.telegram.converter.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.time.format.DateTimeFormatter;

@Component
public class ConversionStatsCommand implements BotCommand {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ConversionQueueService queueService;

    private UserService userService;

    private LocalisationService localisationService;

    private MessageService messageService;

    @Autowired
    public ConversionStatsCommand(ConversionQueueService queueService, UserService userService,
                                  LocalisationService localisationService, @Qualifier("messageLimits") MessageService messageService) {
        this.queueService = queueService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.messageService = messageService;
    }

    @Override
    public boolean accept(Message message) {
        return userService.isAdmin(message.getFrom().getId());
    }

    @Override
    public void processMessage(Message message, String[] params) {
        ConversionQueueItem item = queueService.getItem(Integer.parseInt(params[0]));

        if (item != null) {
            String status = item.getStatus().name();
            String createdAt = DATE_TIME_FORMATTER.format(item.getCreatedAt());
            String startedAt = DATE_TIME_FORMATTER.format(item.getStatedAt());
            String lastRunAt = "";
            if (item.getLastRunAt() != null) {
                lastRunAt = DATE_TIME_FORMATTER.format(item.getLastRunAt());
            }
            String completedAt = "";
            if (item.getCompletedAt() != null) {
                completedAt = DATE_TIME_FORMATTER.format(item.getCompletedAt());
            }
            String resultFileId = "";
            if (StringUtils.isNotBlank(item.getResultFileId())) {
                resultFileId = item.getResultFileId();
            }
            String msg = localisationService.getMessage(MessagesProperties.MESSAGE_CONVERSION_STATS, new Object[]{
                    status, item.getFirstFileFormat().name(), item.getTargetFormat().name(), createdAt, startedAt, lastRunAt,
                    completedAt, item.getFirstFileId(), resultFileId
            }, userService.getLocaleOrDefault(message.getFrom().getId()));

            messageService.sendMessage(new HtmlMessage(message.getChatId(), msg));
        }
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.CONVERSION_STATS_COMMAND;
    }
}

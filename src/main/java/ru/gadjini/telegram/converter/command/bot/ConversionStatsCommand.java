package ru.gadjini.telegram.converter.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.WorkQueueService;

import java.time.format.DateTimeFormatter;

@Component
public class ConversionStatsCommand implements BotCommand {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private WorkQueueService queueService;

    private UserService userService;

    private LocalisationService localisationService;

    private MessageService messageService;

    @Autowired
    public ConversionStatsCommand(WorkQueueService queueService, UserService userService,
                                  LocalisationService localisationService, @TgMessageLimitsControl MessageService messageService) {
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
        ConversionQueueItem item = (ConversionQueueItem) queueService.getById(Integer.parseInt(params[0]));

        if (item != null) {
            String status = item.getStatus().name();
            String createdAt = DATE_TIME_FORMATTER.format(item.getCreatedAt());
            String startedAt = DATE_TIME_FORMATTER.format(item.getStartedAt());
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
            String msg = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONVERSION_STATS, new Object[]{
                    status, item.getFirstFileFormat().name(), item.getTargetFormat().name(), createdAt, startedAt, lastRunAt,
                    completedAt, item.getFirstFileId(), resultFileId
            }, userService.getLocaleOrDefault(message.getFrom().getId()));

            messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId())).text(msg).parseMode(ParseMode.HTML).build());
        }
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.CONVERSION_STATS_COMMAND;
    }
}

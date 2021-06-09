package ru.gadjini.telegram.converter.command.bot.merge;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.job.WorkQueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.Objects;

public class MergeFilesCommand implements BotCommand, NavigableBotCommand {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private MessageMediaService messageMediaService;

    private CommandStateService commandStateService;

    private WorkQueueJob workQueueJob;

    private ConvertionService convertionService;

    private ApplicationProperties applicationProperties;

    private MergeFilesConfigurator mergeFilesConfigurator;

    public MergeFilesCommand(MessageService messageService, LocalisationService localisationService,
                             UserService userService, ConverterReplyKeyboardService replyKeyboardService,
                             MessageMediaService messageMediaService, CommandStateService commandStateService,
                             ConvertionService convertionService, ApplicationProperties applicationProperties,
                             MergeFilesConfigurator mergeFilesConfigurator) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.messageMediaService = messageMediaService;
        this.commandStateService = commandStateService;
        this.convertionService = convertionService;
        this.applicationProperties = applicationProperties;
        this.mergeFilesConfigurator = mergeFilesConfigurator;
    }

    @Autowired
    public void setWorkQueueJob(WorkQueueJob workQueueJob) {
        this.workQueueJob = workQueueJob;
    }

    @Override
    public boolean accept(Message message) {
        return mergeFilesConfigurator.accept(message);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_FILES_WELCOME,
                                new Object[]{mergeFilesConfigurator.getFileType()}, locale))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(replyKeyboardService.mergeFilesKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public String getCommandIdentifier() {
        return mergeFilesConfigurator.getCommandName();
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return mergeFilesConfigurator.getCommandName();
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        if (message.hasText()) {
            String mergeCommandName = localisationService.getMessage(ConverterMessagesProperties.MERGE_COMMAND_NAME, locale);
            String cancelFilesCommandName = localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale);
            ConvertState mergeState = commandStateService.getState(message.getChatId(), getCommandIdentifier(), false, ConvertState.class);
            if (mergeState == null || mergeState.getFiles().isEmpty()) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_NO_FILES, locale));
            }
            if (Objects.equals(mergeCommandName, text)) {
                if (mergeState.getFiles().size() == 1) {
                    throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_MIN_2_FILES, locale));
                }
                workQueueJob.cancelCurrentTasks(message.getChatId());
                convertionService.createConversion(message.getFrom(), mergeState, mergeFilesConfigurator.getTargetFormat(), locale);
                commandStateService.deleteState(message.getChatId(), mergeFilesConfigurator.getCommandName());
            } else if (Objects.equals(cancelFilesCommandName, text)) {
                commandStateService.deleteState(message.getChatId(), mergeFilesConfigurator.getCommandName());
                messageService.sendMessage(
                        SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_CANCELED, new Object[]{mergeState.getFiles().size()}, locale))
                                .parseMode(ParseMode.HTML)
                                .build()
                );
            }
        } else {
            ConvertState mergeState = commandStateService.getState(message.getChatId(), getCommandIdentifier(), false, ConvertState.class);
            if (mergeState == null) {
                mergeState = createState(message);
                commandStateService.setState(message.getChatId(), getCommandIdentifier(), mergeState);
            } else {
                MessageMedia media = messageMediaService.getMedia(message, locale);
                if (media != null && mergeFilesConfigurator.isValidFormat(media.getFormat())) {
                    mergeState.addMedia(media);
                    commandStateService.setState(message.getChatId(), getCommandIdentifier(), mergeState);
                }
            }
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getCommandIdentifier());
    }

    private ConvertState createState(Message message) {
        ConvertState mergeState = new ConvertState();
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        mergeState.setUserLanguage(locale.getLanguage());
        mergeState.setMessageId(message.getMessageId());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        mergeState.addMedia(media);

        return mergeState;
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || !mergeFilesConfigurator.isValidFormat(media.getFormat())) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_INCORRECT_FILE,
                    new Object[]{mergeFilesConfigurator.getFileType()}, locale));
        }
    }
}

package ru.gadjini.telegram.converter.command.bot.cut;

import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
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
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

import static ru.gadjini.telegram.converter.service.conversion.impl.VideoCutter.PERIOD_FORMATTER;

public class CutMediaCommand implements BotCommand, NavigableBotCommand {

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private MessageMediaService messageMediaService;

    private WorkQueueJob workQueueJob;

    private ConvertionService convertionService;

    private CutMediaConfigurator cutMediaConfigurator;

    public CutMediaCommand(MessageService messageService, UserService userService,
                           LocalisationService localisationService,
                           ConverterReplyKeyboardService replyKeyboardService,
                           CommandStateService commandStateService,
                           MessageMediaService messageMediaService,
                           ConvertionService convertionService,
                           CutMediaConfigurator cutMediaConfigurator) {
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.messageMediaService = messageMediaService;
        this.convertionService = convertionService;
        this.cutMediaConfigurator = cutMediaConfigurator;
    }

    @Override
    public boolean accept(Message message) {
        return cutMediaConfigurator.accept(message);
    }

    @Autowired
    public void setWorkQueueJob(WorkQueueJob workQueueJob) {
        this.workQueueJob = workQueueJob;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MEDIA_CUT_WELCOME,
                                new Object[]{cutMediaConfigurator.getMediaTypeName(locale)}, locale))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(replyKeyboardService.mediaCutKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public String getCommandIdentifier() {
        return cutMediaConfigurator.getCommandName();
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return cutMediaConfigurator.getCommandName();
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        ConvertState existsState = commandStateService.getState(message.getChatId(),
                cutMediaConfigurator.getCommandName(), false, ConvertState.class);
        if (existsState == null) {
            ConvertState convertState = createState(message, locale);
            messageService.sendMessage(
                    SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                            .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MEDIA_CUT_START_POINT, locale))
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(replyKeyboardService.mediaCutKeyboard(message.getChatId(), locale))
                            .build()
            );
            commandStateService.setState(message.getChatId(), getCommandIdentifier(), convertState);
        } else if (message.hasText()) {
            if (Format.PROBE.getName().equals(text)) {
                workQueueJob.cancelCurrentTasks(message.getChatId());
                convertionService.createConversion(message.getFrom(), existsState, Format.PROBE, locale);
            } else if (localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILE_COMMAND_NAME, locale).equals(text)) {
                commandStateService.deleteState(message.getChatId(), cutMediaConfigurator.getCommandName());
                messageService.sendMessage(
                        SendMessage.builder()
                                .chatId(String.valueOf(message.getChatId()))
                                .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MEDIA_CUT_WELCOME,
                                        new Object[]{cutMediaConfigurator.getMediaTypeName(locale)}, locale))
                                .replyMarkup(replyKeyboardService.mediaCutKeyboard(message.getChatId(), locale))
                                .build()
                );
            } else {
                if (existsState.getSettings().getCutStartPoint() == null) {
                    Period startPoint = parsePeriod(text, ConverterMessagesProperties.MESSAGE_MEDIA_CUT_START_POINT, locale);
                    existsState.getSettings().setCutStartPoint(startPoint);
                    messageService.sendMessage(
                            SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MEDIA_CUT_END_POINT, locale))
                                    .parseMode(ParseMode.HTML)
                                    .replyMarkup(replyKeyboardService.mediaCutKeyboard(message.getChatId(), locale))
                                    .build()
                    );
                    commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
                } else {
                    Period endPoint = parsePeriod(text, ConverterMessagesProperties.MESSAGE_MEDIA_CUT_END_POINT, locale);
                    validateEndPoint(existsState.getSettings().getCutStartPoint(), endPoint, locale);
                    existsState.getSettings().setCutEndPoint(endPoint);

                    workQueueJob.cancelCurrentTasks(message.getChatId());
                    convertionService.createConversion(message.getFrom(), existsState, Format.CUT, locale);
                    commandStateService.deleteState(message.getChatId(), cutMediaConfigurator.getCommandName());
                }
            }
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, cutMediaConfigurator.getCommandName());
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        convertState.setMedia(media);

        return convertState;
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat().getCategory() != cutMediaConfigurator.getFormatCategory()) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MEDIA_CUT_WELCOME,
                    new Object[]{cutMediaConfigurator.getMediaTypeName(locale)}, locale));
        }
    }

    private void validateEndPoint(Period startPoint, Period endPoint, Locale locale) {
        if (endPoint.toStandardDuration().getStandardSeconds() <= startPoint.toStandardDuration().getStandardSeconds()) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MEDIA_CUT_INVALID_END_POINT, locale));
        }
    }

    private Period parsePeriod(String candidate, String errorMessageCode, Locale locale) {
        try {
            return PERIOD_FORMATTER.parsePeriod(candidate);
        } catch (IllegalArgumentException e) {
            throw new UserException(localisationService.getMessage(errorMessageCode, locale));
        }
    }
}

package ru.gadjini.telegram.converter.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.job.WorkQueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AudioBassBoostCommand implements BotCommand, NavigableBotCommand, CallbackBotCommand {

    private static final List<String> BASS_BOOST = new ArrayList<>();

    static {
        for (int i = -20; i < 20; i += 2) {
            BASS_BOOST.add(String.valueOf(i));
        }
    }

    private CommandStateService commandStateService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private MessageMediaService messageMediaService;

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    private WorkQueueJob workQueueJob;

    private ConvertionService convertionService;

    @Autowired
    public AudioBassBoostCommand(CommandStateService commandStateService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                 MessageMediaService messageMediaService,
                                 InlineKeyboardService inlineKeyboardService, LocalisationService localisationService,
                                 @TgMessageLimitsControl MessageService messageService, UserService userService, ConvertionService convertionService) {
        this.commandStateService = commandStateService;
        this.replyKeyboardService = replyKeyboardService;
        this.messageMediaService = messageMediaService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        this.convertionService = convertionService;
    }

    @Autowired
    public void setWorkQueueJob(WorkQueueJob workQueueJob) {
        this.workQueueJob = workQueueJob;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return ConverterCommandNames.BASS_BOOST;
    }

    @Override
    public String getName() {
        return getHistoryName();
    }

    @Override
    public String getHistoryName() {
        return ConverterCommandNames.BASS_BOOST;
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.BASS_BOOST;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        messageService.sendMessage(
                SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_BASS_BOOST_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.audioBassBoostKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public void processNonCommandCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.AUDIO_BASS_BOOST.getKey())) {
            setBassBoost(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(),
                    requestParams.getString(ConverterArg.AUDIO_BASS_BOOST.getKey()));
        } else if (requestParams.contains(ConverterArg.BASS_BOOST.getKey())) {
            ConvertState convertState = commandStateService.getState(callbackQuery.getMessage().getChatId(),
                    ConverterCommandNames.BASS_BOOST, true, ConvertState.class);

            if (convertState != null) {
                workQueueJob.cancelCurrentTasks(callbackQuery.getMessage().getChatId());
                convertionService.createConversion(callbackQuery.getFrom(), convertState, Format.BASS_BOOST, new Locale(convertState.getUserLanguage()));
                commandStateService.deleteState(callbackQuery.getMessage().getChatId(), ConverterCommandNames.BASS_BOOST);
                messageService.removeInlineKeyboard(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
            }
        }
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        ConvertState existsState = commandStateService.getState(message.getChatId(),
                ConverterCommandNames.BASS_BOOST, false, ConvertState.class);

        if (existsState == null) {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            ConvertState convertState = createState(message, locale);
            messageService.sendMessage(
                    SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                            .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_CHOOSE_BASS_BOOST, locale))
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(inlineKeyboardService.getBassBoostKeyboard(
                                    convertState.getSettings().getBassBoost(), BASS_BOOST, locale)).build(),
                    sent -> {
                        convertState.getSettings().setMessageId(sent.getMessageId());
                        commandStateService.setState(sent.getChatId(), getCommandIdentifier(), convertState);
                    }
            );
        } else {
            updateState(existsState, message);
            commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
        }
    }

    private void setBassBoost(long userId, int messageId, String bassBoost) {
        Locale locale = userService.getLocaleOrDefault(userId);
        validateBassBoost(bassBoost, locale);

        ConvertState convertState = commandStateService.getState(userId,
                ConverterCommandNames.BASS_BOOST, true, ConvertState.class);

        String oldBassBoost = convertState.getSettings().getBassBoost();

        messageService.editKeyboard(EditMessageReplyMarkup.builder()
                .messageId(messageId)
                .chatId(String.valueOf(userId))
                .replyMarkup(inlineKeyboardService.getBassBoostKeyboard(
                        oldBassBoost, BASS_BOOST, locale))
                .build(), false);
        convertState.getSettings().setBassBoost(bassBoost);
        commandStateService.setState(userId, getHistoryName(), convertState);
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

    private void updateState(ConvertState convertState, Message message) {
        Locale locale = new Locale(convertState.getUserLanguage());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        convertState.setMedia(media);
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat().getCategory() != FormatCategory.AUDIO) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_BASS_BOOST_FILE_NOT_FOUND, locale));
        }
    }

    private void validateBassBoost(String bassBoost, Locale locale) {
        if (!BASS_BOOST.contains(bassBoost)) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_CHOOSE_BASS_BOOST, locale));
        }
    }
}

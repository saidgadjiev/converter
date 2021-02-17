package ru.gadjini.telegram.converter.command.bot;

import com.antkorwin.xsync.XSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class EditVideoCommand implements BotCommand, NavigableBotCommand, CallbackBotCommand {

    private static final String DEFAULT_RESOLUTION = "144p";

    private static final List<String> AVAILABLE_RESOLUTIONS = List.of("720p", "480p", "360p", "240p", DEFAULT_RESOLUTION);

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private XSync<Long> longXSync;

    private CommandStateService commandStateService;

    private MessageMediaService messageMediaService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public EditVideoCommand(@Qualifier("messageLimits") MessageService messageService, UserService userService,
                            LocalisationService localisationService,
                            @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService,
                            XSync<Long> longXSync, CommandStateService commandStateService,
                            MessageMediaService messageMediaService, InlineKeyboardService inlineKeyboardService) {
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.longXSync = longXSync;
        this.commandStateService = commandStateService;
        this.messageMediaService = messageMediaService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_EDIT_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.videoEditKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.EDIT_VIDEO;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return ConverterCommandNames.EDIT_VIDEO;
    }

    @Override
    public String getName() {
        return ConverterCommandNames.EDIT_VIDEO;
    }

    @Override
    public void processNonCommandCallback(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.RESOLUTION.getKey())) {
            String resolution = requestParams.getString(ConverterArg.RESOLUTION.getKey());
            if (AVAILABLE_RESOLUTIONS.contains(resolution)) {
                setResolution(callbackQuery.getMessage().getChatId(), resolution);
            } else {
                //TODO: отправлять сообщение чтобы выбрали разрешение с клавиатуры
            }
        }
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        longXSync.execute(message.getChatId(), () -> {
            ConvertState existsState = commandStateService.getState(message.getChatId(),
                    ConverterCommandNames.EDIT_VIDEO, false, ConvertState.class);

            if (existsState == null) {
                Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
                ConvertState convertState = createState(message, locale);
                messageService.sendMessage(
                        SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(buildSettingsMessage(convertState))
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(inlineKeyboardService.getVideoEditSettingsKeyboard(AVAILABLE_RESOLUTIONS, locale))
                                .build(),
                        sent -> {
                            convertState.getSettings().setMessageId(sent.getMessageId());
                            commandStateService.setState(sent.getChatId(), getCommandIdentifier(), convertState);
                        }
                );
            }
        });
    }

    private void setResolution(long chatId, String resolution) {
        ConvertState convertState = commandStateService.getState(chatId,
                ConverterCommandNames.EDIT_VIDEO, true, ConvertState.class);

        String oldResolution = convertState.getSettings().getResolution();
        convertState.getSettings().setResolution(resolution);
        if (!Objects.equals(resolution, oldResolution)) {
            updateSettingsMessage(chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.EDIT_VIDEO, convertState);
    }

    private void updateSettingsMessage(long chatId, ConvertState convertState) {
        messageService.editMessage(EditMessageText.builder().chatId(String.valueOf(chatId))
                .messageId(convertState.getSettings().getMessageId())
                .text(buildSettingsMessage(convertState))
                .parseMode(ParseMode.HTML)
                .replyMarkup(inlineKeyboardService.getVideoEditSettingsKeyboard(AVAILABLE_RESOLUTIONS, new Locale(convertState.getUserLanguage())))
                .build());
    }

    private String buildSettingsMessage(ConvertState convertState) {
        return localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_EDIT_SETTINGS,
                new Object[]{convertState.getSettings().getResolution()}, new Locale(convertState.getUserLanguage()));
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());
        convertState.getSettings().setResolution(DEFAULT_RESOLUTION);
        MessageMedia media = messageMediaService.getMedia(message, locale);

        convertState.setMedia(media);

        return convertState;
    }
}

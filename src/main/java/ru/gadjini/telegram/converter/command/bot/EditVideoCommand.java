package ru.gadjini.telegram.converter.command.bot;

import com.antkorwin.xsync.XSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
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
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class EditVideoCommand implements BotCommand, NavigableBotCommand, CallbackBotCommand {

    private static final String DEFAULT_RESOLUTION = "144p";

    private static final List<String> AVAILABLE_RESOLUTIONS = List.of("1080p", "720p", "480p", "360p", "240p", DEFAULT_RESOLUTION, "64p", "32p", "16p");

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private XSync<Long> longXSync;

    private CommandStateService commandStateService;

    private MessageMediaService messageMediaService;

    private InlineKeyboardService inlineKeyboardService;

    private WorkQueueJob workQueueJob;

    private ConvertionService convertionService;

    @Value("${converter:all}")
    private String converter;

    @Autowired
    public EditVideoCommand(@TgMessageLimitsControl MessageService messageService, UserService userService,
                            LocalisationService localisationService,
                            @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                            XSync<Long> longXSync, CommandStateService commandStateService,
                            MessageMediaService messageMediaService, InlineKeyboardService inlineKeyboardService,
                            ConvertionService convertionService) {
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.longXSync = longXSync;
        this.commandStateService = commandStateService;
        this.messageMediaService = messageMediaService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.convertionService = convertionService;
    }

    @Override
    public boolean accept(Message message) {
        return FormatsConfiguration.ALL_CONVERTER.equals(converter) || FormatsConfiguration.VIDEO_CONVERTER.equals(converter);
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
    public void processNonCommandCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.RESOLUTION.getKey())) {
            String resolution = requestParams.getString(ConverterArg.RESOLUTION.getKey());

            String answerCallbackQuery;
            if (AVAILABLE_RESOLUTIONS.contains(resolution)) {
                setResolution(callbackQuery.getMessage().getChatId(), resolution);
                answerCallbackQuery = localisationService.getMessage(MessagesProperties.MESSAGE_RESOLUTION_SELECTED,
                        userService.getLocaleOrDefault(callbackQuery.getFrom().getId()));
            } else {
                answerCallbackQuery = localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_VIDEO_RESOLUTION,
                        userService.getLocaleOrDefault(callbackQuery.getFrom().getId()));
            }
            messageService.sendAnswerCallbackQuery(
                    AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text(answerCallbackQuery)
                            .build()
            );
        } else if (requestParams.contains(ConverterArg.EDIT_VIDEO.getKey())) {
            ConvertState convertState = commandStateService.getState(callbackQuery.getMessage().getChatId(),
                    ConverterCommandNames.EDIT_VIDEO, true, ConvertState.class);

            workQueueJob.cancelCurrentTasks(callbackQuery.getMessage().getChatId());
            convertionService.createConversion(callbackQuery.getFrom(), convertState, Format.EDIT, new Locale(convertState.getUserLanguage()));
            commandStateService.deleteState(callbackQuery.getMessage().getChatId(), ConverterCommandNames.EDIT_VIDEO);
            messageService.removeInlineKeyboard(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
        }
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        longXSync.execute(message.getChatId(), () -> {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            ConvertState existsState = commandStateService.getState(message.getChatId(),
                    ConverterCommandNames.EDIT_VIDEO, false, ConvertState.class);
            if (message.hasText()) {
                if (existsState == null) {
                    messageService.sendMessage(
                            SendMessage.builder()
                                    .chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_SEND_VIDEO_TO_EDIT, locale))
                                    .build()
                    );
                } else {
                    messageService.sendMessage(
                            SendMessage.builder()
                                    .chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_VIDEO_RESOLUTION, locale))
                                    .build()
                    );
                }
            } else {
                if (existsState == null) {
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
                } else {
                    updateState(existsState, message);
                    updateSettingsMessage(message.getChatId(), existsState);
                    commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
                }
            }
        });
    }

    @Override
    public void leave(long chatId) {
        ConvertState state = commandStateService.getState(chatId, ConverterCommandNames.EDIT_VIDEO, false, ConvertState.class);
        if (state != null) {
            commandStateService.deleteState(chatId, ConverterCommandNames.EDIT_VIDEO);
            messageService.removeInlineKeyboard(chatId, state.getSettings().getMessageId());
        }
    }

    private void updateState(ConvertState convertState, Message message) {
        Locale locale = new Locale(convertState.getUserLanguage());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        convertState.setMedia(media);
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
        StringBuilder message = new StringBuilder();

        Locale locale = new Locale(convertState.getUserLanguage());
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_EDIT_SETTINGS_RESOLUTION,
                new Object[]{convertState.getSettings().getResolution()}, locale));
        message.append("\n").append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_FORMAT,
                new Object[]{convertState.getFirstFormat().getName()}, locale));

        message.append("\n\n").append(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_RESOLUTION_WARN, locale));

        message.append("\n\n").append(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_EDIT_SETTINGS_CHOOSE_RESOLUTION, locale));

        return message.toString();
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());
        convertState.getSettings().setResolution(DEFAULT_RESOLUTION);
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        convertState.setMedia(media);

        return convertState;
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat().getCategory() != FormatCategory.VIDEO) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SEND_VIDEO_TO_EDIT, locale));
        }
    }
}

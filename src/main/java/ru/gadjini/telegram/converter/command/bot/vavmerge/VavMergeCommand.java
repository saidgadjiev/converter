package ru.gadjini.telegram.converter.command.bot.vavmerge;

import org.apache.commons.lang3.StringUtils;
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
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.conversion.impl.VavMergeConverter;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
@SuppressWarnings("CPD-START")
public class VavMergeCommand implements NavigableBotCommand, BotCommand, CallbackBotCommand {

    private static final int AUDIOS_COUNT = 3;

    private static final int SUBTITLES_COUNT = 3;

    private static final Set<FormatCategory> ACCEPT_CATEGORIES = Set.of(FormatCategory.VIDEO, FormatCategory.AUDIO, FormatCategory.SUBTITLES);

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private MessageMediaService messageMediaService;

    private ConvertionService convertionService;

    private ApplicationProperties applicationProperties;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public VavMergeCommand(@TgMessageLimitsControl MessageService messageService,
                           LocalisationService localisationService, UserService userService,
                           @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                           CommandStateService commandStateService, MessageMediaService messageMediaService,
                           ConvertionService convertionService, ApplicationProperties applicationProperties,
                           InlineKeyboardService inlineKeyboardService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.messageMediaService = messageMediaService;
        this.convertionService = convertionService;
        this.applicationProperties = applicationProperties;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getCommandWelcomeMessage(getCommandIdentifier(),
                                ConverterMessagesProperties.MESSAGE_VAVMERGE_WELCOME,
                                new Object[]{AUDIOS_COUNT, SUBTITLES_COUNT}, locale))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(replyKeyboardService.vavmergeKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        VavMergeState existsState = commandStateService.getState(message.getChatId(), getCommandIdentifier(), false, VavMergeState.class);

        if (existsState == null) {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            VavMergeState convertState = createState(message, locale);
            commandStateService.setState(message.getChatId(), getCommandIdentifier(), convertState);
        } else {
            Locale locale = new Locale(existsState.getUserLanguage());
            MessageMedia media = messageMediaService.getMedia(message, locale);
            if (media != null) {
                addMedia(existsState, media);
                commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
            } else if (message.hasText()) {
                String cancelFilesCommand = localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale);
                String vmakeCommand = localisationService.getMessage(ConverterMessagesProperties.VAVMERGE_COMMAND_NAME, locale);
                if (Objects.equals(text, cancelFilesCommand)) {
                    messageService.sendMessage(
                            SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VAVMERGE_FILES_CANCELED, locale))
                                    .parseMode(ParseMode.HTML)
                                    .replyMarkup(replyKeyboardService.vavmergeKeyboard(message.getChatId(), locale))
                                    .build()
                    );
                    commandStateService.deleteState(message.getChatId(), getCommandIdentifier());
                } else if (Objects.equals(text, vmakeCommand)) {
                    validateVavMerge(existsState);
                    messageService.sendMessage(
                            SendMessage.builder()
                                    .chatId(String.valueOf(message.getChatId()))
                                    .parseMode(ParseMode.HTML)
                                    .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_SETTINGS,
                                            new Object[]{existsState.getAudios().size(), existsState.getSubtitles().size()}, locale))
                                    .replyMarkup(inlineKeyboardService.getVavMergeSettingsKeyboard(existsState, locale))
                                    .build()
                    );
                }
            }
        }
    }

    @Override
    public void processNonCommandCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.VAV_MERGE_AUDIO_MODE.getKey())) {
            VavMergeState existsState = commandStateService.getState(callbackQuery.getFrom().getId(),
                    getCommandIdentifier(), true, VavMergeState.class);
            existsState.setAudioMode(requestParams.getString(ConverterArg.VAV_MERGE_AUDIO_MODE.getKey()));
            messageService.editKeyboard(callbackQuery.getMessage().getReplyMarkup(),
                    EditMessageReplyMarkup.builder().chatId(String.valueOf(callbackQuery.getFrom().getId()))
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .replyMarkup(inlineKeyboardService.getVavMergeSettingsKeyboard(existsState,
                                    new Locale(existsState.getUserLanguage()))).build());
            commandStateService.setState(callbackQuery.getFrom().getId(), getCommandIdentifier(), existsState);
        } else if (requestParams.contains(ConverterArg.VAV_MERGE_SUBTITLES_MODE.getKey())) {
            VavMergeState existsState = commandStateService.getState(callbackQuery.getFrom().getId(),
                    getCommandIdentifier(), true, VavMergeState.class);
            existsState.setSubtitlesMode(requestParams.getString(ConverterArg.VAV_MERGE_SUBTITLES_MODE.getKey()));

            messageService.editKeyboard(callbackQuery.getMessage().getReplyMarkup(),
                    EditMessageReplyMarkup.builder().chatId(String.valueOf(callbackQuery.getFrom().getId()))
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .replyMarkup(inlineKeyboardService.getVavMergeSettingsKeyboard(existsState,
                                    new Locale(existsState.getUserLanguage()))).build());
            commandStateService.setState(callbackQuery.getFrom().getId(), getCommandIdentifier(), existsState);
        } else if (requestParams.contains(ConverterArg.VAV_MERGE.getKey())) {
            VavMergeState existsState = commandStateService.getState(callbackQuery.getFrom().getId(),
                    getCommandIdentifier(), true, VavMergeState.class);

            validateVavMerge(existsState);
            ConvertState convertState = toConvertState(existsState);
            convertionService.createConversion(callbackQuery.getFrom(), convertState, Format.MERGE, new Locale(existsState.getUserLanguage()));
            commandStateService.deleteState(callbackQuery.getFrom().getId(), getCommandIdentifier());
            messageService.removeInlineKeyboard(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
        }
    }

    @Override
    public String getName() {
        return ConverterCommandNames.VAVMERGE;
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.VAVMERGE;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return getCommandIdentifier();
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getCommandIdentifier());
    }

    private VavMergeState createState(Message message, Locale locale) {
        VavMergeState convertState = new VavMergeState(AUDIOS_COUNT, SUBTITLES_COUNT);
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setAudioMode(VavMergeConverter.ADD_AUDIO_MODE);
        convertState.setSubtitlesMode(VavMergeConverter.ADD_SUBTITLES_MODE);
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        addMedia(convertState, media);

        return convertState;
    }

    private void addMedia(VavMergeState vavMergeState, MessageMedia media) {
        if (media.getFormat().getCategory() == FormatCategory.VIDEO) {
            vavMergeState.setVideo(media);
        } else if (media.getFormat().getCategory() == FormatCategory.AUDIO) {
            vavMergeState.addAudio(media);
        } else {
            vavMergeState.addSubtitles(media);
        }
    }

    private void validateVavMerge(VavMergeState convertState) {
        String awaitingMessage = getAwaitingMessage(convertState);

        if (StringUtils.isNotBlank(awaitingMessage)) {
            throw new UserException(awaitingMessage);
        }
    }

    private String getAwaitingMessage(VavMergeState convertState) {
        if (convertState.getVideo() == null) {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VAVMERGE_AWAITING_VIDEO, new Locale(convertState.getUserLanguage()));
        }
        if (convertState.getAudios() == null && convertState.getSubtitles() == null) {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VAVMERGE_AWAITING_AUDIO_SUBTITLES, new Locale(convertState.getUserLanguage()));
        }

        return null;
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || !ACCEPT_CATEGORIES.contains(media.getFormat().getCategory())) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VAVMERGE_WELCOME,
                    new Object[]{AUDIOS_COUNT, SUBTITLES_COUNT}, locale));
        }
    }

    private ConvertState toConvertState(VavMergeState vavMergeState) {
        ConvertState convertState = new ConvertState();
        convertState.addMedia(vavMergeState.getVideo());
        vavMergeState.getAudios().forEach(convertState::addMedia);
        vavMergeState.getSubtitles().forEach(convertState::addMedia);
        convertState.setUserLanguage(vavMergeState.getUserLanguage());
        convertState.setMessageId(vavMergeState.getMessageId());
        convertState.setSettings(new SettingsState());
        convertState.getSettings().setVavMergeAudioMode(vavMergeState.getAudioMode());
        convertState.getSettings().setVavMergeSubtitlesMode(vavMergeState.getSubtitlesMode());

        return convertState;
    }
}

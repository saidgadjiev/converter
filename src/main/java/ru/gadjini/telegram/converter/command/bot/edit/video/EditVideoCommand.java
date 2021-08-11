package ru.gadjini.telegram.converter.command.bot.edit.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.*;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.job.WorkQueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;
import java.util.Set;

@Component
public class EditVideoCommand implements BotCommand, NavigableBotCommand, CallbackBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditVideoCommand.class);

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private MessageMediaService messageMediaService;

    private WorkQueueJob workQueueJob;

    private ConvertionService convertionService;

    private ApplicationProperties applicationProperties;

    private Set<EditVideoSettingsState> editVideoSettingsStateSet;

    @Autowired
    public EditVideoCommand(@TgMessageLimitsControl MessageService messageService, UserService userService,
                            LocalisationService localisationService,
                            @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                            CommandStateService commandStateService,
                            MessageMediaService messageMediaService,
                            ConvertionService convertionService, ApplicationProperties applicationProperties) {
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.messageMediaService = messageMediaService;
        this.convertionService = convertionService;
        this.applicationProperties = applicationProperties;
    }

    @Autowired
    public void setEditVideoSettingsStateSet(Set<EditVideoSettingsState> editVideoSettingsStateSet) {
        this.editVideoSettingsStateSet = editVideoSettingsStateSet;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER);
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
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_EDIT_WELCOME, locale))
                        .parseMode(ParseMode.HTML)
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
        EditVideoState state = commandStateService.getState(callbackQuery.getFrom().getId(),
                getCommandIdentifier(), true, EditVideoState.class);
        getState(state.getStateName()).callbackUpdate(this, callbackQuery, requestParams, state);
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        EditVideoState existsState = commandStateService.getState(message.getChatId(),
                ConverterCommandNames.EDIT_VIDEO, false, EditVideoState.class);

        if (existsState == null) {
            EditVideoState convertState = createState(message, locale);
            getState(convertState.getStateName()).enter(this, message, convertState);
        } else {
            MessageMedia messageMedia = messageMediaService.getMedia(message, locale);
            if (messageMedia != null) {
                checkMedia(messageMedia, locale);
                existsState.setMedia(messageMedia);
                commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
            } else if (message.hasText()) {
                if (Format.PROBE.getName().equals(text)) {
                    workQueueJob.cancelCurrentTasks(message.getChatId());
                    convertionService.createConversion(message.getFrom(), existsState.getState(), Format.PROBE, locale);
                } else {
                    messageService.sendMessage(
                            SendMessage.builder()
                                    .chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_VIDEO_RESOLUTION, locale))
                                    .build()
                    );
                }
            }
        }
    }

    @Override
    public void leave(long chatId) {
        EditVideoState state = commandStateService.getState(chatId, ConverterCommandNames.EDIT_VIDEO, false, EditVideoState.class);
        if (state != null) {
            commandStateService.deleteState(chatId, ConverterCommandNames.EDIT_VIDEO);
            messageService.removeInlineKeyboard(chatId, state.getSettings().getMessageId());
        }
    }

    private EditVideoState createState(Message message, Locale locale) {
        EditVideoState editVideoState = new EditVideoState();
        editVideoState.setState(new ConvertState());
        editVideoState.setStateName(EditVideoSettingsStateName.WELCOME);
        ConvertState convertState = editVideoState.getState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());
        convertState.getSettings().setResolution(EditVideoResolutionState.DEFAULT_RESOLUTION);
        convertState.getSettings().setCrf(EditVideoCrfState.DEFAULT_CRF);
        convertState.getSettings().setAudioCodec(EditVideoAudioCodecState.AUTO);
        MessageMedia media = messageMediaService.getMedia(message, locale);

        LOGGER.debug("Edit video state({}, {})", message.getChatId(), TgMessage.getMetaTypes(message));

        checkMedia(media, locale);
        convertState.setMedia(media);

        return editVideoState;
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat() == null || media.getFormat().getCategory() != FormatCategory.VIDEO) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SEND_VIDEO_TO_EDIT, locale));
        }
    }

    private EditVideoSettingsState getState(EditVideoSettingsStateName settingsStateName) {
        return editVideoSettingsStateSet.stream().filter(s -> settingsStateName.equals(s.getName())).findFirst().orElseThrow();
    }
}

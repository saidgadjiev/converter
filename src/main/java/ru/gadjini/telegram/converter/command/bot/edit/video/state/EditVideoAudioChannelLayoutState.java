package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.command.bot.edit.video.EditVideoCommand;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@SuppressWarnings("CPD-START")
public class EditVideoAudioChannelLayoutState extends BaseEditVideoState {

    public static final String AUTO = "x";

    public static final String MONO = "mono";

    public static final String STEREO = "stereo";

    public static final List<String> AVAILABLE_AUDIO_MONO_STEREO = List.of(AUTO, MONO, STEREO);

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private EditVideoSettingsWelcomeState welcomeState;

    @Autowired
    public EditVideoAudioChannelLayoutState(@TgMessageLimitsControl MessageService messageService, InlineKeyboardService inlineKeyboardService,
                                            CommandStateService commandStateService, LocalisationService localisationService) {
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
    }

    @Autowired
    public void setWelcomeState(EditVideoSettingsWelcomeState welcomeState) {
        this.welcomeState = welcomeState;
    }

    @Override
    public boolean enter(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery, EditVideoState currentState) {
        Locale locale = new Locale(currentState.getUserLanguage());
        if (!currentState.hasAudio()) {
            sendVideoHasNoAudioAnswer(callbackQuery, locale);

            return false;
        }
        messageService.editMessage(
                callbackQuery.getMessage().getText(),
                callbackQuery.getMessage().getReplyMarkup(),
                EditMessageText.builder()
                        .chatId(String.valueOf(callbackQuery.getFrom().getId()))
                        .text(buildSettingsMessage(currentState))
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .replyMarkup(inlineKeyboardService.getVideoEditAudioMonoStereoKeyboard(
                                currentState.getSettings().getAudioChannelLayout(),
                                AVAILABLE_AUDIO_MONO_STEREO, locale))
                        .build()
        );
        return true;
    }

    @Override
    public void callbackUpdate(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery,
                               RequestParams requestParams, EditVideoState currentState) {
        if (requestParams.contains(ConverterArg.GO_BACK.getKey())) {
            currentState.setStateName(welcomeState.getName());
            welcomeState.goBack(editVideoCommand, callbackQuery, currentState);
            commandStateService.setState(callbackQuery.getFrom().getId(), editVideoCommand.getCommandIdentifier(), currentState);
        } else if (requestParams.contains(ConverterArg.AUDIO_MONO_STEREO.getKey())) {
            Locale locale = new Locale(currentState.getUserLanguage());
            if (!currentState.hasAudio()) {
                messageService.sendAnswerCallbackQuery(
                        AnswerCallbackQuery.builder()
                                .callbackQueryId(callbackQuery.getId())
                                .text(localisationService.getMessage(
                                        ConverterMessagesProperties.MESSAGE_NO_AUDIO_IN_VIDEO,
                                        locale
                                ))
                                .showAlert(true)
                                .build()
                );
            } else {
                String audioBitrate = requestParams.getString(ConverterArg.AUDIO_MONO_STEREO.getKey());
                String answerCallbackQuery;
                if (AVAILABLE_AUDIO_MONO_STEREO.contains(audioBitrate)) {
                    setAudioMonoStereo(callbackQuery, audioBitrate);
                    answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SELECTED,
                            locale);
                } else {
                    answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_VIDEO_AUDIO_MONO_STEREO,
                            locale);
                }
                messageService.sendAnswerCallbackQuery(
                        AnswerCallbackQuery.builder()
                                .callbackQueryId(callbackQuery.getId())
                                .text(answerCallbackQuery)
                                .build()
                );
            }
        }
    }

    @Override
    public EditVideoSettingsStateName getName() {
        return EditVideoSettingsStateName.AUDIO_CHANNEL_LAYOUT;
    }

    private void setAudioMonoStereo(CallbackQuery callbackQuery, String audioMonoStereo) {
        long chatId = callbackQuery.getFrom().getId();
        EditVideoState convertState = commandStateService.getState(chatId,
                ConverterCommandNames.EDIT_VIDEO, true, EditVideoState.class);

        String oldAudioMonoStereo = convertState.getSettings().getAudioChannelLayout();
        convertState.getSettings().setAudioChannelLayout(audioMonoStereo);
        if (!Objects.equals(audioMonoStereo, oldAudioMonoStereo)) {
            updateSettingsMessage(callbackQuery, chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.EDIT_VIDEO, convertState);
    }
}

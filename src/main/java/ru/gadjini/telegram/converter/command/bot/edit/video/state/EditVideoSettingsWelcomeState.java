package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.edit.video.EditVideoCommand;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.job.WorkQueueJob;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Component
public class EditVideoSettingsWelcomeState extends BaseEditVideoState {

    private CommandStateService commandStateService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private EditVideoResolutionState resolutionState;

    private LocalisationService localisationService;

    private EditVideoQualityState crfState;

    private EditVideoAudioCodecState audioCodecState;

    private WorkQueueJob workQueueJob;

    private ConvertionService convertionService;

    private EditVideoAudioBitrateState audioBitrateState;

    private EditVideoAudioChannelLayoutState monoStereoState;

    @Autowired
    public EditVideoSettingsWelcomeState(CommandStateService commandStateService,
                                         @TgMessageLimitsControl MessageService messageService,
                                         InlineKeyboardService inlineKeyboardService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Autowired
    public void setMonoStereoState(EditVideoAudioChannelLayoutState monoStereoState) {
        this.monoStereoState = monoStereoState;
    }

    @Autowired
    public void setAudioBitrateState(EditVideoAudioBitrateState audioBitrateState) {
        this.audioBitrateState = audioBitrateState;
    }

    @Autowired
    public void setAudioCodecState(EditVideoAudioCodecState audioCodecState) {
        this.audioCodecState = audioCodecState;
    }

    @Autowired
    public void setLocalService(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Autowired
    public void setCommandStateService(ConvertionService convertionService) {
        this.convertionService = convertionService;
    }

    @Autowired
    public void setWorkQueueJob(WorkQueueJob workQueueJob) {
        this.workQueueJob = workQueueJob;
    }

    @Autowired
    public void setCrfState(EditVideoQualityState crfState) {
        this.crfState = crfState;
    }

    @Autowired
    public void setResolutionState(EditVideoResolutionState resolutionState) {
        this.resolutionState = resolutionState;
    }

    @Override
    public void goBack(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery, EditVideoState currentState) {
        messageService.editMessage(
                callbackQuery.getMessage().getText(),
                callbackQuery.getMessage().getReplyMarkup(),
                EditMessageText.builder()
                        .chatId(String.valueOf(callbackQuery.getFrom().getId()))
                        .text(buildSettingsMessage(currentState.getState()))
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .replyMarkup(inlineKeyboardService.getVideoEditSettingsKeyboard(new Locale(currentState.getUserLanguage())))
                        .build()
        );
    }

    @Override
    public void enter(EditVideoCommand editVideoCommand, Message message, EditVideoState editVideoState) {
        messageService.sendMessage(
                SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                        .text(buildSettingsMessage(editVideoState.getState()))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(inlineKeyboardService.getVideoEditSettingsKeyboard(
                                new Locale(editVideoState.getUserLanguage())))
                        .build(),
                sent -> {
                    editVideoState.getSettings().setMessageId(sent.getMessageId());
                    commandStateService.setState(sent.getChatId(), editVideoCommand.getCommandIdentifier(), editVideoState);
                }
        );
    }

    @Override
    public void callbackUpdate(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery,
                               RequestParams requestParams, EditVideoState currentState) {
        if (requestParams.contains(ConverterArg.VEDIT_CHOOSE_RESOLUTION.getKey())) {
            currentState.setStateName(resolutionState.getName());
            resolutionState.enter(editVideoCommand, callbackQuery, currentState);
            commandStateService.setState(callbackQuery.getFrom().getId(), editVideoCommand.getCommandIdentifier(), currentState);
        } else if (requestParams.contains(ConverterArg.VEDIT_CHOOSE_CRF.getKey())) {
            currentState.setStateName(crfState.getName());
            crfState.enter(editVideoCommand, callbackQuery, currentState);
            commandStateService.setState(callbackQuery.getFrom().getId(), editVideoCommand.getCommandIdentifier(), currentState);
        } else if (requestParams.contains(ConverterArg.VEDIT_CHOOSE_AUDIO_CODEC.getKey())) {
            currentState.setStateName(audioCodecState.getName());
            audioCodecState.enter(editVideoCommand, callbackQuery, currentState);
            commandStateService.setState(callbackQuery.getFrom().getId(), editVideoCommand.getCommandIdentifier(), currentState);
        } else if (requestParams.contains(ConverterArg.VEDIT_CHOOSE_AUDIO_BITRATE.getKey())) {
            currentState.setStateName(audioBitrateState.getName());
            audioBitrateState.enter(editVideoCommand, callbackQuery, currentState);
            commandStateService.setState(callbackQuery.getFrom().getId(), editVideoCommand.getCommandIdentifier(), currentState);
        } else if (requestParams.contains(ConverterArg.VEDIT_CHOOSE_MONO_STEREO.getKey())) {
            currentState.setStateName(monoStereoState.getName());
            monoStereoState.enter(editVideoCommand, callbackQuery, currentState);
            commandStateService.setState(callbackQuery.getFrom().getId(), editVideoCommand.getCommandIdentifier(), currentState);
        } else if (requestParams.contains(ConverterArg.EDIT_VIDEO.getKey())) {
            EditVideoState editVideoState = commandStateService.getState(callbackQuery.getFrom().getId(),
                    ConverterCommandNames.EDIT_VIDEO, true, EditVideoState.class);

            if (validate(callbackQuery.getId(), editVideoState)) {
                workQueueJob.cancelCurrentTasks(callbackQuery.getFrom().getId());
                convertionService.createConversion(callbackQuery.getFrom(), editVideoState.getState(), Format.EDIT,
                        new Locale(editVideoState.getState().getUserLanguage()));
                commandStateService.deleteState(callbackQuery.getFrom().getId(), ConverterCommandNames.EDIT_VIDEO);
                messageService.removeInlineKeyboard(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            }
        }
    }

    @Override
    public EditVideoSettingsStateName getName() {
        return EditVideoSettingsStateName.WELCOME;
    }

    private boolean validate(String queryId, EditVideoState editVideoState) {
        if (EditVideoResolutionState.AUTO.equals(editVideoState.getSettings().getResolution())
                && "110".equals(editVideoState.getSettings().getCrf())
                && EditVideoAudioCodecState.AUTO.equals(editVideoState.getSettings().getAudioCodec())
                && EditVideoAudioBitrateState.AUTO.equals(editVideoState.getSettings().getAudioBitrate())
                && EditVideoAudioChannelLayoutState.AUTO.equals(editVideoState.getSettings().getAudioChannelLayout())) {
            messageService.sendAnswerCallbackQuery(
                    AnswerCallbackQuery.builder()
                            .callbackQueryId(queryId)
                            .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_EDIT_SETTINGS,
                                    new Locale(editVideoState.getUserLanguage())))
                            .showAlert(true).build()
            );

            return false;
        }

        return true;
    }
}

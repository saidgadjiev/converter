package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.command.bot.edit.video.EditVideoCommand;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.bitrate.AudioCompressionHelper;
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
public class EditVideoResolutionState extends BaseEditVideoState {

    public static final String AUTO = "x";

    public static final List<Integer> AVAILABLE_RESOLUTIONS = List.of(1080, 720, 480, 360, 240, 144);

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private EditVideoSettingsWelcomeState welcomeState;

    @Autowired
    public EditVideoResolutionState(@TgMessageLimitsControl MessageService messageService,
                                    InlineKeyboardService inlineKeyboardService, CommandStateService commandStateService,
                                    LocalisationService localisationService) {
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
        messageService.editMessage(
                callbackQuery.getMessage().getText(),
                callbackQuery.getMessage().getReplyMarkup(),
                EditMessageText.builder()
                        .chatId(String.valueOf(callbackQuery.getFrom().getId()))
                        .text(buildSettingsMessage(currentState))
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .replyMarkup(inlineKeyboardService.getVideoEditResolutionsKeyboard(currentState.getSettings().getResolution(),
                                AVAILABLE_RESOLUTIONS, currentState.getCurrentVideoResolution(), new Locale(currentState.getUserLanguage())))
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
        } else if (requestParams.contains(ConverterArg.RESOLUTION.getKey())) {
            String resolution = requestParams.getString(ConverterArg.RESOLUTION.getKey());
            Locale locale = new Locale(currentState.getUserLanguage());
            String answerCallbackQuery;
            boolean showAlert = true;
            if (isValid(resolution)) {
                if (isGteThanSource(resolution, currentState.getCurrentVideoResolution())) {
                    answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_RESOLUTION_CANT_BE_INCREASED,
                            new Object[]{
                                    String.valueOf(currentState.getCurrentVideoResolution())
                            },
                            locale);
                } else {
                    setResolution(currentState, callbackQuery, resolution);
                    if (AUTO.equals(resolution)) {
                        answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SELECTED,
                                locale);
                        showAlert = false;
                    } else {
                        answerCallbackQuery = getResolutionSelectedMessage(currentState, locale);
                    }
                }
            } else {
                answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VEDIT_CHOOSE_RESOLUTION,
                        locale);
            }
            messageService.sendAnswerCallbackQuery(
                    AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text(answerCallbackQuery)
                            .showAlert(showAlert)
                            .build()
            );
        }
    }

    @Override
    public EditVideoSettingsStateName getName() {
        return EditVideoSettingsStateName.RESOLUTION;
    }

    private void setResolution(EditVideoState convertState, CallbackQuery callbackQuery, String resolution) {
        long chatId = callbackQuery.getFrom().getId();

        String oldResolution = convertState.getSettings().getResolution();
        convertState.getSettings().setResolution(resolution);
        setQualityByResolution(convertState, resolution);

        if (!Objects.equals(resolution, oldResolution)) {
            updateSettingsMessage(callbackQuery, chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.EDIT_VIDEO, convertState);
    }

    private void setQualityByResolution(EditVideoState editVideoState, String resolution) {
        if (resolution.equals(AUTO)) {
            editVideoState.getSettings().setAudioBitrate(EditVideoAudioBitrateState.AUTO);
        } else if (editVideoState.hasAudio()) {
            int res = Integer.parseInt(resolution);
            Integer audioBitrate = AudioCompressionHelper.getAudioBitrateForCompression(res, editVideoState.getCurrentAudioBitrate());

            if (audioBitrate != null) {
                editVideoState.getSettings().setAudioBitrate(String.valueOf(audioBitrate));
            }
            editVideoState.getSettings().setCompressBy("26");
        }
    }

    private boolean isValid(String resolution) {
        if (resolution.equals(AUTO)) {
            return true;
        }
        try {
            return AVAILABLE_RESOLUTIONS.contains(Integer.parseInt(resolution));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isGteThanSource(String resolution, Integer currentResolution) {
        if (resolution.equals(AUTO)) {
            return false;
        }
        if (currentResolution == null) {
            return false;
        }

        return Integer.parseInt(resolution) >= currentResolution;
    }

    private String getResolutionSelectedMessage(EditVideoState currentState, Locale locale) {
        return currentState.hasAudio() || StringUtils.isBlank(currentState.getSettings().getAudioBitrate())
                ? localisationService.getMessage(ConverterMessagesProperties.MESSAGE_RESOLUTION_SELECTED,
                new Object[]{
                        currentState.getSettings().getCompressBy(),
                        currentState.getSettings().getAudioBitrateInKBytes() + "k",
                },
                locale)
                :
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_RESOLUTION_SELECTED_NO_AUDIO,
                        new Object[]{
                                currentState.getSettings().getCompressBy()
                        },
                        locale);
    }
}

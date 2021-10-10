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
import java.util.Map;
import java.util.Objects;

@Component
public class EditVideoResolutionState extends BaseEditVideoState {

    public static final String AUTO = "x";

    static final List<Integer> AVAILABLE_RESOLUTIONS = List.of(1080, 720, 480, 360, 240, 144);

    private static final Map<Integer, Integer> BITRATE_BY_RESOLUTION = Map.of(
            1080, 3000 * 1024,
            720, 1500 * 1024,
            480, 500 * 1024,
            360, 400 * 1024,
            240, 300 * 1024,
            144, 200 * 1024
    );

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
    public void enter(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery, EditVideoState currentState) {
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
            if (isValid(resolution)) {
                setResolution(callbackQuery, resolution);
                answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SELECTED,
                        locale);
            } else {
                answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_VIDEO_EDIT_SETTINGS,
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

    @Override
    public EditVideoSettingsStateName getName() {
        return EditVideoSettingsStateName.RESOLUTION;
    }

    private void setResolution(CallbackQuery callbackQuery, String resolution) {
        long chatId = callbackQuery.getFrom().getId();
        EditVideoState convertState = commandStateService.getState(chatId,
                ConverterCommandNames.EDIT_VIDEO, true, EditVideoState.class);

        String oldResolution = convertState.getSettings().getResolution();
        convertState.getSettings().setResolution(resolution);
        String qualityByResolution = getQualityByResolution(convertState, resolution);
        convertState.getSettings().setCrf(qualityByResolution);

        if (!Objects.equals(resolution, oldResolution)) {
            updateSettingsMessage(callbackQuery, chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.EDIT_VIDEO, convertState);
    }

    private String getQualityByResolution(EditVideoState editVideoState, String resolution) {
        Integer res = Integer.parseInt(resolution);
        Integer bitrate = BITRATE_BY_RESOLUTION.get(res);
        double factor = editVideoState.getCurrentVideoBitrate().doubleValue() / bitrate;

        int quality = (int) (EditVideoQualityState.MAX_QUALITY / factor);

        return String.valueOf(EditVideoQualityState.MAX_QUALITY - quality);
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
}

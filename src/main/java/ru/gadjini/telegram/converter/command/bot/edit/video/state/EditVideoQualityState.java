package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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
public class EditVideoQualityState extends BaseEditVideoState {

    public static final String AUTO = "x";

    public static final String DEFAULT_QUALITY = "30";

    public static final int MAX_COMPRESSION_RATE = 90;

    static final Integer MAX_QUALITY = 100;

    static final List<Integer> AVAILABLE_QUALITIES = List.of(10, 20, 30, 40, 50, 60, 70, 80, 90);

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private EditVideoSettingsWelcomeState welcomeState;

    @Autowired
    public EditVideoQualityState(@TgMessageLimitsControl MessageService messageService, InlineKeyboardService inlineKeyboardService,
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
    public void enter(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery, EditVideoState currentState) {
        messageService.editMessage(
                callbackQuery.getMessage().getText(),
                callbackQuery.getMessage().getReplyMarkup(),
                EditMessageText.builder()
                        .chatId(String.valueOf(callbackQuery.getFrom().getId()))
                        .text(buildSettingsMessage(currentState))
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .replyMarkup(inlineKeyboardService.getVideoEditCrfKeyboard(currentState.getSettings().getCrf(),
                                AVAILABLE_QUALITIES, new Locale(currentState.getUserLanguage())))
                        .build()
        );
    }

    @Override
    public void callbackUpdate(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery,
                               RequestParams requestParams, EditVideoState currentState) {
        if (requestParams.contains(ConverterArg.CRF_MODE.getKey())) {
            messageService.editKeyboard(
                    callbackQuery.getMessage().getReplyMarkup(),
                    EditMessageReplyMarkup.builder()
                            .chatId(String.valueOf(callbackQuery.getFrom().getId()))
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .replyMarkup(inlineKeyboardService.getVideoEditCrfKeyboard(currentState.getSettings().getCrf(),
                                    AVAILABLE_QUALITIES, new Locale(currentState.getUserLanguage())))
                            .build()
            );
        } else if (requestParams.contains(ConverterArg.GO_BACK.getKey())) {
            currentState.setStateName(welcomeState.getName());
            welcomeState.goBack(editVideoCommand, callbackQuery, currentState);
            commandStateService.setState(callbackQuery.getFrom().getId(), editVideoCommand.getCommandIdentifier(), currentState);
        } else if (requestParams.contains(ConverterArg.CRF.getKey())) {
            String crf = requestParams.getString(ConverterArg.CRF.getKey());
            Locale locale = new Locale(currentState.getUserLanguage());
            String answerCallbackQuery;
            if (isValid(crf)) {
                setCrf(callbackQuery, crf);
                answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SELECTED,
                        locale);
            } else {
                answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_VIDEO_CRF,
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
        return EditVideoSettingsStateName.CRF;
    }

    private void setCrf(CallbackQuery callbackQuery, String crf) {
        long chatId = callbackQuery.getFrom().getId();
        EditVideoState convertState = commandStateService.getState(chatId,
                ConverterCommandNames.EDIT_VIDEO, true, EditVideoState.class);

        String oldCrf = convertState.getSettings().getCrf();
        convertState.getSettings().setCrf(crf);
        if (!Objects.equals(crf, oldCrf)) {
            updateSettingsMessage(callbackQuery, chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.EDIT_VIDEO, convertState);
    }

    private boolean isValid(String crf) {
        if (crf.equals(AUTO)) {
            return true;
        }
        try {
            return AVAILABLE_QUALITIES.contains(Integer.parseInt(crf));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EditVideoQualityState extends BaseEditVideoState {

    public static final String AUTO = "x";

    public static final String DEFAULT_QUALITY = "30";

    public static final int MAX_COMPRESSION_RATE = 90;

    static final Integer MAX_QUALITY = 100;

    static final int MIN_QUALITY = 0;

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
                        .replyMarkup(inlineKeyboardService.getVideoEditQualityKeyboard(QualityCalculator.getCompressionRate(currentState),
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
                            .replyMarkup(inlineKeyboardService.getVideoEditQualityKeyboard(QualityCalculator.getCompressionRate(currentState),
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
                setQuality(callbackQuery, crf, currentState);
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

    private void setQuality(CallbackQuery callbackQuery, String compressionRate, EditVideoState convertState) {
        long chatId = callbackQuery.getFrom().getId();

        if (compressionRate.equals(AUTO)) {
            convertState.getSettings().setVideoBitrate(convertState.getCurrentVideoBitrate());
        } else {
            int targetOverallBitrate = convertState.getCurrentOverallBitrate() * Integer.parseInt(compressionRate)
                    / EditVideoQualityState.MAX_QUALITY;
            int targetAudioBitrate = findTargetAudioBitrate(convertState.getSettings().getResolutionOrDefault(convertState.getCurrentVideoResolution()));
            AtomicInteger videoBitrate = new AtomicInteger();
            AtomicInteger audioBitrate = new AtomicInteger();
            VideoAudioBitrateCalculator.calculateVideoAudioBitrate(convertState.getCurrentOverallBitrate(),
                    convertState.getCurrentVideoBitrate(), targetOverallBitrate, targetAudioBitrate, convertState.getCurrentAudioBitrate(),
                    videoBitrate, audioBitrate);
            convertState.getSettings().setAudioBitrateIfNotSetYet(String.valueOf(audioBitrate.get()));
            convertState.getSettings().setVideoBitrate(videoBitrate.get());
        }

        String oldQuality = convertState.getSettings().getQuality();
        convertState.getSettings().setQuality(String.valueOf(QualityCalculator.getQuality(convertState)));
        if (!Objects.equals(oldQuality, convertState.getSettings().getQuality())) {
            updateSettingsMessage(callbackQuery, chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.EDIT_VIDEO, convertState);
    }

    private int findTargetAudioBitrate(int resolution) {
        if (EditVideoResolutionState.AUDIO_BITRATE_BY_RESOLUTION.containsKey(resolution)) {
            return EditVideoResolutionState.AUDIO_BITRATE_BY_RESOLUTION.get(resolution);
        }
        List<Integer> resolutions = new ArrayList<>(EditVideoResolutionState.VIDEO_BITRATE_BY_RESOLUTION.keySet());
        int distance = Math.abs(resolutions.get(0) - resolution);
        int idx = 0;
        for (int c = 1; c < resolutions.size(); c++) {
            int cdistance = Math.abs(resolutions.get(c) - resolution);
            if (cdistance < distance) {
                idx = c;
                distance = cdistance;
            }
        }

        return EditVideoResolutionState.AUDIO_BITRATE_BY_RESOLUTION.get(resolutions.get(idx));
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

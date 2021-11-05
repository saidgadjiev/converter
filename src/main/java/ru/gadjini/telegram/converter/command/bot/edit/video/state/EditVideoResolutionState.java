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
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EditVideoResolutionState extends BaseEditVideoState {

    public static final String AUTO = "x";

    public static final int MIN_RESOLUTION = 144;

    static final List<Integer> AVAILABLE_RESOLUTIONS = List.of(1080, 720, 480, 360, 240, 144);

    public static final Map<Integer, Integer> VIDEO_BITRATE_BY_RESOLUTION = Map.of(
            1080, 3872 * 1024,
            720, 2436 * 1024,
            480, 1136 * 1024,
            360, 636 * 1024,
            240, 368 * 1024,
            144, 218 * 1024
    );

    public static final Map<Integer, Integer> AUDIO_BITRATE_BY_RESOLUTION = Map.of(
            1080, 128 * 1024,
            720, 64 * 1024,
            480, 64 * 1024,
            360, 64 * 1024,
            240, 32 * 1024,
            144, 32 * 1024
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
                        answerCallbackQuery = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_RESOLUTION_SELECTED,
                                new Object[]{
                                        currentState.getSettings().getParsedCompressBy(),
                                        currentState.getSettings().getAudioBitrateInKBytes() + "k",
                                        getEstimatedSize(currentState.getFirstFile().getFileSize(), QualityCalculator.getQuality(currentState))
                                },
                                locale);
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
            editVideoState.getSettings().setVideoBitrate(editVideoState.getCurrentVideoBitrate());
            editVideoState.getSettings().setCompressBy(EditVideoQualityState.AUTO);
            editVideoState.getSettings().setAudioBitrate(EditVideoAudioBitrateState.AUTO);
        } else {
            int res = Integer.parseInt(resolution);
            int videoBitrate = VIDEO_BITRATE_BY_RESOLUTION.get(res);
            int audioBitrate = AUDIO_BITRATE_BY_RESOLUTION.get(res);
            int quality = QualityCalculator.getQuality(editVideoState, videoBitrate, audioBitrate);

            if (quality >= EditVideoQualityState.MAX_QUALITY) {
                int minimumQuality = findMinimumQuality(editVideoState, res);
                int targetOverallBitrate = editVideoState.getCurrentOverallBitrate() * minimumQuality
                        / EditVideoQualityState.MAX_QUALITY;
                int targetAudioBitrate = AUDIO_BITRATE_BY_RESOLUTION.get(res);

                AtomicInteger resultVideoBitrate = new AtomicInteger();
                AtomicInteger resultAudioBitrate = new AtomicInteger();
                VideoAudioBitrateCalculator.calculateVideoAudioBitrate(editVideoState.getCurrentOverallBitrate(),
                        editVideoState.getCurrentVideoBitrate(), targetOverallBitrate, targetAudioBitrate,
                        editVideoState.getCurrentAudioBitrate(),
                        resultVideoBitrate, resultAudioBitrate, AUDIO_BITRATE_BY_RESOLUTION.values());
                editVideoState.getSettings().setVideoBitrate(resultVideoBitrate.get());
                editVideoState.getSettings().setAudioBitrate(String.valueOf(resultAudioBitrate.get()));
                editVideoState.getSettings().setCompressBy(String.valueOf(EditVideoQualityState.MAX_QUALITY - minimumQuality));
            } else {
                editVideoState.getSettings().setVideoBitrate(videoBitrate);
                editVideoState.getSettings().setAudioBitrate(String.valueOf(audioBitrate));
                editVideoState.getSettings().setCompressBy(String.valueOf(EditVideoQualityState.MAX_QUALITY - QualityCalculator.getQuality(editVideoState)));
            }
        }
    }

    private int findMinimumQuality(EditVideoState editVideoState, int res) {
        int max = 70;
        for (Map.Entry<Integer, Integer> entry : VIDEO_BITRATE_BY_RESOLUTION.entrySet()) {
            if (entry.getKey() < res) {
                int r = entry.getKey();
                int videoBitrate = VIDEO_BITRATE_BY_RESOLUTION.get(r);
                int audioBitrate = AUDIO_BITRATE_BY_RESOLUTION.get(r);
                int quality = QualityCalculator.getQuality(editVideoState, videoBitrate, audioBitrate);

                if (quality > max) {
                    max = quality;
                }
            }
        }

        return max;
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
}

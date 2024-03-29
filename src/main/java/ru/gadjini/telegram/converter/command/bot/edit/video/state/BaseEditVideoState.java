package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.utils.BitrateUtils;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.Locale;

public abstract class BaseEditVideoState implements EditVideoSettingsState {

    private LocalisationService localisationService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public void setMessageService(@TgMessageLimitsControl MessageService messageService) {
        this.messageService = messageService;
    }

    @Autowired
    public void setInlineKeyboardService(InlineKeyboardService inlineKeyboardService) {
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Autowired
    public void setLocalisationService(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    final String buildSettingsMessage(EditVideoState editVideoState) {
        StringBuilder message = new StringBuilder();
        ConvertState convertState = editVideoState.getState();

        Locale locale = new Locale(convertState.getUserLanguage());
        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_EDIT_SETTINGS,
                new Object[]{convertState.getFirstFormat().getName(),
                        MemoryUtils.humanReadableByteCount(convertState.getFirstFile().getFileSize()),
                        editVideoState.getCurrentVideoResolution() + "p",
                        getResolutionMessage(convertState.getSettings().getResolution(), locale),
                        getQualityMessage(editVideoState.getSettings().getCompressBy(), locale),
                        getAudioCodecMessage(convertState.getSettings().getAudioCodec(), locale),
                        getAudioBitrateMessage(convertState.getSettings().getAudioBitrate(), locale),
                        getAudioMonoStereoMessage(convertState.getSettings().getAudioChannelLayout(), locale)},
                locale));

        switch (getName()) {
            case WELCOME:
                message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_EDIT_CHOOSE_SETTINGS, locale));
                break;
            case RESOLUTION:
                message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VEDIT_CHOOSE_RESOLUTION, locale));
                break;
            case CRF:
                message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VEDIT_CHOOSE_COMPRESSION_RATE, locale));
                break;
            case AUDIO_CODEC:
                message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VEDIT_CHOOSE_AUDIO_CODEC, locale));
                break;
            case AUDIO_BITRATE:
                message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VEDIT_CHOOSE_AUDIO_BITRATE, locale));
                break;
            case AUDIO_CHANNEL_LAYOUT:
                message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VEDIT_CHOOSE_AUDIO_CHANNEL_LAYOUT, locale));
                break;
        }

        return message.toString();
    }

    final void updateSettingsMessage(CallbackQuery callbackQuery, long chatId, EditVideoState editVideoState) {
        messageService.editMessage(callbackQuery.getMessage().getText(),
                callbackQuery.getMessage().getReplyMarkup(), EditMessageText.builder().chatId(String.valueOf(chatId))
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .text(buildSettingsMessage(editVideoState))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(getMarkup(editVideoState))
                        .build());
    }

    private String getResolutionMessage(String resolution, Locale locale) {
        return EditVideoResolutionState.AUTO.equals(resolution) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_CHANGE, locale) : resolution + "p";
    }


    private String getQualityMessage(String crf, Locale locale) {
        return EditVideoResolutionState.AUTO.equals(crf) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_CHANGE, locale) : crf;
    }

    private String getAudioBitrateMessage(String audioBitrate, Locale locale) {
        return EditVideoAudioBitrateState.AUTO.equals(audioBitrate) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUTO, locale)
                : BitrateUtils.toKBytes(Integer.parseInt(audioBitrate)) + "k";
    }

    private String getAudioMonoStereoMessage(String monoStereo, Locale locale) {
        return EditVideoAudioChannelLayoutState.AUTO.equals(monoStereo) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUTO, locale) : monoStereo;
    }

    private String getAudioCodecMessage(String audioCodec, Locale locale) {
        return EditVideoAudioCodecState.AUTO.equals(audioCodec) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUTO, locale) : audioCodec;
    }

    private InlineKeyboardMarkup getMarkup(EditVideoState editVideoState) {
        ConvertState convertState = editVideoState.getState();
        return getName() == EditVideoSettingsStateName.RESOLUTION
                ? inlineKeyboardService.getVideoEditResolutionsKeyboard(convertState.getSettings().getResolution(),
                EditVideoResolutionState.AVAILABLE_RESOLUTIONS, editVideoState.getCurrentVideoResolution(), new Locale(convertState.getUserLanguage()))
                : getName() == EditVideoSettingsStateName.AUDIO_CODEC
                ? inlineKeyboardService.getVideoEditAudioCodecsKeyboard(convertState.getSettings().getAudioCodec(),
                EditVideoAudioCodecState.AVAILABLE_AUDIO_CODECS, new Locale(convertState.getUserLanguage()))
                : getName() == EditVideoSettingsStateName.AUDIO_BITRATE
                ? inlineKeyboardService.getVideoEditAudioBitrateKeyboard(convertState.getSettings().getAudioBitrateInKBytes(),
                EditVideoAudioBitrateState.AVAILABLE_AUDIO_BITRATES, new Locale(convertState.getUserLanguage()))
                : getName() == EditVideoSettingsStateName.AUDIO_CHANNEL_LAYOUT
                ? inlineKeyboardService.getVideoEditAudioMonoStereoKeyboard(convertState.getSettings().getAudioChannelLayout(),
                EditVideoAudioChannelLayoutState.AVAILABLE_AUDIO_MONO_STEREO, new Locale(convertState.getUserLanguage()))
                : inlineKeyboardService.getVideoEditQualityKeyboard(editVideoState.getSettings().getCompressBy(),
                EditVideoQualityState.AVAILABLE_CRF, new Locale(convertState.getUserLanguage()));
    }

    final void sendVideoHasNoAudioAnswer(CallbackQuery callbackQuery, Locale locale) {
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
    }

}

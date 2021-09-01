package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

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

    final String buildSettingsMessage(ConvertState convertState) {
        StringBuilder message = new StringBuilder();

        Locale locale = new Locale(convertState.getUserLanguage());
        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_EDIT_SETTINGS,
                new Object[]{getResolutionMessage(convertState.getSettings().getResolution(), locale),
                        getCrfMessage(convertState.getSettings().getCrf(), locale),
                        getAudioCodecMessage(convertState.getSettings().getAudioCodec(), locale),
                        getAudioBitrateMessage(convertState.getSettings().getAudioBitrate(), locale),
                        getAudioMonoStereoMessage(convertState.getSettings().getMonoStereo(), locale)}, locale));
        message.append("\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILE_FORMAT,
                new Object[]{convertState.getFirstFormat().getName()}, locale));

        message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_RESOLUTION_WARN, locale));
        message.append("\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_CRF_WARN, locale));

        message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_EDIT_CHOOSE_SETTINGS, locale));

        return message.toString();
    }

    final void updateSettingsMessage(CallbackQuery callbackQuery, long chatId, ConvertState convertState) {
        messageService.editMessage(callbackQuery.getMessage().getText(),
                callbackQuery.getMessage().getReplyMarkup(), EditMessageText.builder().chatId(String.valueOf(chatId))
                        .messageId(convertState.getSettings().getMessageId())
                        .text(buildSettingsMessage(convertState))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(getMarkup(convertState))
                        .build());
    }

    private String getResolutionMessage(String resolution, Locale locale) {
        return EditVideoResolutionState.DONT_CHANGE.equals(resolution) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_CHANGE, locale) : resolution;
    }

    private String getCrfMessage(String crf, Locale locale) {
        return EditVideoResolutionState.DONT_CHANGE.equals(crf) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_CHANGE, locale) : crf;
    }

    private String getAudioBitrateMessage(String audioBitrate, Locale locale) {
        return EditVideoAudioCodecState.AUTO.equals(audioBitrate) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUTO, locale) : audioBitrate + "k";
    }

    private String getAudioMonoStereoMessage(String monoStereo, Locale locale) {
        return EditVideoAudioCodecState.AUTO.equals(monoStereo) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUTO, locale) : monoStereo + "k";
    }

    private String getAudioCodecMessage(String audioCodec, Locale locale) {
        return EditVideoAudioCodecState.AUTO.equals(audioCodec) ?
                localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUTO, locale) : audioCodec;
    }

    private InlineKeyboardMarkup getMarkup(ConvertState convertState) {
        return getName() == EditVideoSettingsStateName.RESOLUTION
                ? inlineKeyboardService.getVideoEditResolutionsKeyboard(convertState.getSettings().getResolution(),
                EditVideoResolutionState.AVAILABLE_RESOLUTIONS, new Locale(convertState.getUserLanguage()))
                : getName() == EditVideoSettingsStateName.AUDIO_CODEC
                ? inlineKeyboardService.getVideoEditAudioCodecsKeyboard(convertState.getSettings().getAudioCodec(),
                EditVideoAudioCodecState.AVAILABLE_AUDIO_CODECS, new Locale(convertState.getUserLanguage()))
                : getName() == EditVideoSettingsStateName.AUDIO_BITRATE
                ? inlineKeyboardService.getVideoEditAudioBitratesKeyboard(convertState.getSettings().getAudioCodec(),
                EditVideoAudioBitrateState.AVAILABLE_AUDIO_BITRATES, new Locale(convertState.getUserLanguage()))
                : inlineKeyboardService.getVideoEditCrfKeyboard(convertState.getSettings().getCrf(),
                EditVideoCrfState.AVAILABLE_CRF, new Locale(convertState.getUserLanguage()));
    }
}

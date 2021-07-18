package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
                new Object[]{convertState.getSettings().getResolution(), convertState.getSettings().getCrf()}, locale));
        message.append("\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILE_FORMAT,
                new Object[]{convertState.getFirstFormat().getName()}, locale));

        message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_RESOLUTION_WARN, locale));
        message.append("\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_CRF_WARN, locale));

        message.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_EDIT_CHOOSE_SETTINGS, locale));

        return message.toString();
    }

    final void updateSettingsMessage(long chatId, ConvertState convertState) {
        messageService.editMessage(EditMessageText.builder().chatId(String.valueOf(chatId))
                .messageId(convertState.getSettings().getMessageId())
                .text(buildSettingsMessage(convertState))
                .parseMode(ParseMode.HTML)
                .replyMarkup(getName() == EditVideoSettingsStateName.RESOLUTION
                        ? inlineKeyboardService.getVideoEditResolutionsKeyboard(convertState.getSettings().getResolution(),
                        EditVideoResolutionState.AVAILABLE_RESOLUTIONS, new Locale(convertState.getUserLanguage()))
                        : inlineKeyboardService.getVideoEditCrfKeyboard(convertState.getSettings().getCrf(),
                        EditVideoCrfState.AVAILABLE_CRF, new Locale(convertState.getUserLanguage())))
                .build());
    }
}

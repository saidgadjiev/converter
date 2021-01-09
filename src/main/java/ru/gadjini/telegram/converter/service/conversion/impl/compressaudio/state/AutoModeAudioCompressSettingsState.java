package ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.AudioCompressionMode;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class AutoModeAudioCompressSettingsState implements AudioCompressSettingsState {

    private MessageService messageService;

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private UserService userService;

    private InlineKeyboardService inlineKeyboardService;

    private ManuallyModeAudioCompressSettingsState manuallyModeAudioCompressSettingsState;

    @Autowired
    public AutoModeAudioCompressSettingsState(@Qualifier("messageLimits") MessageService messageService,
                                              CommandStateService commandStateService,
                                              LocalisationService localisationService, UserService userService,
                                              InlineKeyboardService inlineKeyboardService) {
        this.messageService = messageService;
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Autowired
    public void setManuallyModeAudioCompressSettingsState(ManuallyModeAudioCompressSettingsState manuallyModeAudioCompressSettingsState) {
        this.manuallyModeAudioCompressSettingsState = manuallyModeAudioCompressSettingsState;
    }

    @Override
    public AudioCompressSettingsStateName getName() {
        return AudioCompressSettingsStateName.AUTO_MODE;
    }

    @Override
    public void mode(CallbackQuery callbackQuery, AudioCompressionMode audioCompressionMode) {
        if (audioCompressionMode.equals(AudioCompressionMode.MANUALLY)) {
            manuallyModeAudioCompressSettingsState.enter(callbackQuery.getMessage().getChatId());
        } else {
            messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder().callbackQueryId(callbackQuery.getId())
                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_MODE_AUTO_CHOSE,
                            userService.getLocaleOrDefault(callbackQuery.getFrom().getId()))).build());
        }
    }

    @Override
    public void enter(long chatId) {
        ConvertState state = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);
        if (!AudioCompressionMode.AUTO.equals(state.getAudioCompressionMode())) {
            Locale locale = userService.getLocaleOrDefault((int) chatId);
            messageService.editMessage(
                    EditMessageText.builder().messageId(state.getAudioCompressSettingsMessageId())
                            .chatId(String.valueOf(chatId))
                            .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_SETTINGS,
                                    new Object[]{AudioCompressionMode.AUTO.name(), AudioCompressionMode.AUTO.name()}, locale))
                            .replyMarkup(inlineKeyboardService.getAudioCompressionSettingsKeyboard(locale))
                            .build()
            );
        }

        state.setAudioCompressSettingsStateName(AudioCompressSettingsStateName.AUTO_MODE);
        state.setAudioCompressionMode(AudioCompressionMode.AUTO);
        commandStateService.setState(chatId, ConverterCommandNames.COMPRESS_AUDIO, state);
    }
}

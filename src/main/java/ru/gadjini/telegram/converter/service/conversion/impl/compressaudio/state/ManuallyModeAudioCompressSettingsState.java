package ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
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
public class ManuallyModeAudioCompressSettingsState implements AudioCompressSettingsState {

    private static final String UNDEFINED_BITRATE = "undefined";

    private AutoModeAudioCompressSettingsState audioCompressSettingsState;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    public ManuallyModeAudioCompressSettingsState(AutoModeAudioCompressSettingsState audioCompressSettingsState,
                                                  CommandStateService commandStateService,
                                                  InlineKeyboardService inlineKeyboardService, LocalisationService localisationService,
                                                  @Qualifier("messageLimits") MessageService messageService, UserService userService) {
        this.audioCompressSettingsState = audioCompressSettingsState;
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
    }

    @Autowired
    public void setAudioCompressSettingsState(AutoModeAudioCompressSettingsState audioCompressSettingsState) {
        this.audioCompressSettingsState = audioCompressSettingsState;
    }

    @Override
    public AudioCompressSettingsStateName getName() {
        return AudioCompressSettingsStateName.MANUALLY_MODE;
    }

    @Override
    public void mode(CallbackQuery callbackQuery, AudioCompressionMode audioCompressionMode) {
        if (audioCompressionMode.equals(AudioCompressionMode.AUTO)) {
            audioCompressSettingsState.enter(callbackQuery.getMessage().getChatId());
        }
    }

    @Override
    public void goBack(long chatId) {
        audioCompressSettingsState.enter(chatId);
    }

    @Override
    public void bitrate(long chatId, String bitrate) {
        ConvertState state = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);
        state.setAudioCompressionBitRate(bitrate);
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        messageService.editMessage(
                EditMessageText.builder().messageId(state.getAudioCompressSettingsMessageId())
                        .chatId(String.valueOf(chatId))
                        .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_SETTINGS,
                                new Object[]{AudioCompressionMode.MANUALLY.name(), bitrate + "k"}, locale))
                        .replyMarkup(inlineKeyboardService.getManuallyCompressionSettingsKeyboard(locale))
                        .build()
        );
    }

    @Override
    public void enter(long chatId) {
        ConvertState state = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);
        if (!AudioCompressionMode.MANUALLY.equals(state.getAudioCompressionMode())) {
            Locale locale = userService.getLocaleOrDefault((int) chatId);
            messageService.editMessage(
                    EditMessageText.builder().messageId(state.getAudioCompressSettingsMessageId())
                            .chatId(String.valueOf(chatId))
                            .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_SETTINGS,
                                    new Object[]{AudioCompressionMode.MANUALLY.name(), UNDEFINED_BITRATE}, locale))
                            .replyMarkup(inlineKeyboardService.getManuallyCompressionSettingsKeyboard(locale))
                            .build()
            );
        }

        state.setAudioCompressSettingsStateName(AudioCompressSettingsStateName.MANUALLY_MODE);
        state.setAudioCompressionMode(AudioCompressionMode.MANUALLY);
        commandStateService.setState(chatId, ConverterCommandNames.COMPRESS_AUDIO, state);
    }
}

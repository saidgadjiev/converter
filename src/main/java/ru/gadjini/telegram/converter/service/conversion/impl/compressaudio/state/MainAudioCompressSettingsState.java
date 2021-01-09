package ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.AudioCompressionMode;

@Component
public class MainAudioCompressSettingsState implements AudioCompressSettingsState {

    private AutoModeAudioCompressSettingsState audioCompressSettingsState;

    private ManuallyModeAudioCompressSettingsState manuallyModeAudioCompressSettingsState;

    @Autowired
    public MainAudioCompressSettingsState(AutoModeAudioCompressSettingsState audioCompressSettingsState,
                                          ManuallyModeAudioCompressSettingsState manuallyModeAudioCompressSettingsState) {
        this.audioCompressSettingsState = audioCompressSettingsState;
        this.manuallyModeAudioCompressSettingsState = manuallyModeAudioCompressSettingsState;
    }

    @Override
    public AudioCompressSettingsStateName getName() {
        return AudioCompressSettingsStateName.MAIN;
    }

    @Override
    public void mode(CallbackQuery callbackQuery, AudioCompressionMode audioCompressionMode) {
        switch (audioCompressionMode) {
            case AUTO:
                audioCompressSettingsState.enter(callbackQuery.getMessage().getChatId());
                break;
            case MANUALLY:
                manuallyModeAudioCompressSettingsState.enter(callbackQuery.getMessage().getChatId());
                break;
        }
    }
}

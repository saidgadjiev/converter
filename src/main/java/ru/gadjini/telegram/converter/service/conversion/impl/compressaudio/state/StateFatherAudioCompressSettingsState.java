package ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.AudioCompressionMode;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;

import java.util.Set;

@Service
public class StateFatherAudioCompressSettingsState implements AudioCompressSettingsState {

    private Set<AudioCompressSettingsState> states;

    private CommandStateService commandStateService;

    @Autowired
    public StateFatherAudioCompressSettingsState(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setStates(Set<AudioCompressSettingsState> states) {
        this.states = states;
    }

    @Override
    public AudioCompressSettingsStateName getName() {
        return AudioCompressSettingsStateName.FATHER;
    }

    @Override
    public void mode(CallbackQuery callbackQuery, AudioCompressionMode audioCompressionMode) {
        AudioCompressSettingsState currentState = getCurrentState(callbackQuery.getMessage().getChatId());

        currentState.mode(callbackQuery, audioCompressionMode);
    }

    @Override
    public void bitrate(long chatId, String bitrate) {
        AudioCompressSettingsState currentState = getCurrentState(chatId);

        currentState.bitrate(chatId, bitrate);
    }

    @Override
    public void goBack(long chatId) {
        AudioCompressSettingsState currentState = getCurrentState(chatId);

        currentState.goBack(chatId);
    }

    private AudioCompressSettingsState getCurrentState(long chatId) {
        ConvertState convertState = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);

        return findState(convertState.getAudioCompressSettingsStateName());
    }

    private AudioCompressSettingsState findState(AudioCompressSettingsStateName name) {
        return states.stream().filter(state -> state.getName().equals(name)).findFirst().orElseThrow();
    }
}

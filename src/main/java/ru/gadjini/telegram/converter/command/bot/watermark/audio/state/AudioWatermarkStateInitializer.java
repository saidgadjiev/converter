package ru.gadjini.telegram.converter.command.bot.watermark.audio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.AMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.settings.AudioWatermarkSettings;
import ru.gadjini.telegram.converter.service.watermark.audio.AudioWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;

@Component
@SuppressWarnings("CPD-START")
public class AudioWatermarkStateInitializer {

    private CommandStateService commandStateService;

    private AudioWatermarkService audioWatermarkService;

    private AudioWatermarkOkState watermarkOkState;

    private AudioNoWatermarkState noWatermarkState;

    @Autowired
    public AudioWatermarkStateInitializer(CommandStateService commandStateService,
                                          AudioWatermarkService audioWatermarkService) {
        this.commandStateService = commandStateService;
        this.audioWatermarkService = audioWatermarkService;
    }

    @Autowired
    public void setNoWatermarkState(AudioNoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Autowired
    public void setWatermarkOkState(AudioWatermarkOkState watermarkOkState) {
        this.watermarkOkState = watermarkOkState;
    }

    public AudioWatermarkSettings initAndGet(Message message, AMarkCommand vMarkCommand) {
        AudioWatermarkSettings audioWatermarkSettings = new AudioWatermarkSettings();

        init(message, vMarkCommand, audioWatermarkSettings);

        return audioWatermarkSettings;
    }

    public void init(Message message, AMarkCommand vMarkCommand, AudioWatermarkSettings audioWatermarkSettings) {
        if (audioWatermarkService.isExistsWatermark(message.getFrom().getId())) {
            audioWatermarkSettings.setStateName(watermarkOkState.getName());
        } else {
            audioWatermarkSettings.setStateName(noWatermarkState.getName());
        }
        commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), audioWatermarkSettings);
    }
}

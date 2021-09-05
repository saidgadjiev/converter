package ru.gadjini.telegram.converter.command.bot.watermark.audio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.AMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.settings.AudioWatermarkSettings;
import ru.gadjini.telegram.converter.service.watermark.audio.AudioWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;

import java.util.Locale;

public abstract class BaseAudioWatermarkState implements AudioWatermarkState {

    private LocalisationService localisationService;

    private AudioWatermarkStateInitializer audioWatermarkStateInitializer;

    private AudioWatermarkService videoWatermarkService;

    private AudioWatermarkOkState watermarkOkState;

    private AudioNoWatermarkState noWatermarkState;

    private CommandStateService commandStateService;

    private UserService userService;

    @Autowired
    public void setNoWatermarkState(AudioNoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Autowired
    public void setVideoWatermarkService(AudioWatermarkService videoWatermarkService) {
        this.videoWatermarkService = videoWatermarkService;
    }

    @Autowired
    public void setWatermarkOkState(AudioWatermarkOkState watermarkOkState) {
        this.watermarkOkState = watermarkOkState;
    }

    @Autowired
    public void setAudioWatermarkStateInitializer(AudioWatermarkStateInitializer audioWatermarkStateInitializer) {
        this.audioWatermarkStateInitializer = audioWatermarkStateInitializer;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setCommandStateService(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setLocalisationService(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    public final void update(AMarkCommand aMarkCommand, Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        if (localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale).equals(text)) {
            AudioWatermarkSettings audioWatermarkSettings = commandStateService.getState(
                    message.getFrom().getId(), aMarkCommand.getCommandIdentifier(), true, AudioWatermarkSettings.class,
                    () -> audioWatermarkStateInitializer.initAndGet(message, aMarkCommand)
            );

            if (videoWatermarkService.isExistsWatermark(message.getFrom().getId())) {
                audioWatermarkSettings.setStateName(watermarkOkState.getName());
                watermarkOkState.enter(message);
            } else {
                audioWatermarkSettings.setStateName(noWatermarkState.getName());
                noWatermarkState.enter(message, true);
            }
            commandStateService.setState(message.getChatId(), aMarkCommand.getCommandIdentifier(), audioWatermarkSettings);
        } else {
            doUpdate(aMarkCommand, message, text);
        }
    }

    protected abstract void doUpdate(AMarkCommand aMarkCommand, Message message, String text);
}

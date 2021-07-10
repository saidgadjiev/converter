package ru.gadjini.telegram.converter.command.bot.watermark.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;

import java.util.Locale;

public abstract class BaseWatermarkState implements VideoWatermarkState {

    private LocalisationService localisationService;

    private VideoWatermarkStateInitializer videoWatermarkStateInitializer;

    private VideoWatermarkService videoWatermarkService;

    private WatermarkOkState watermarkOkState;

    private NoWatermarkState noWatermarkState;

    private CommandStateService commandStateService;

    private UserService userService;

    @Autowired
    public void setNoWatermarkState(NoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Autowired
    public void setVideoWatermarkService(VideoWatermarkService videoWatermarkService) {
        this.videoWatermarkService = videoWatermarkService;
    }

    @Autowired
    public void setWatermarkOkState(WatermarkOkState watermarkOkState) {
        this.watermarkOkState = watermarkOkState;
    }

    @Autowired
    public void setVideoWatermarkStateInitializer(VideoWatermarkStateInitializer videoWatermarkStateInitializer) {
        this.videoWatermarkStateInitializer = videoWatermarkStateInitializer;
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
    public final void update(VMarkCommand vMarkCommand, Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        if (localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale).equals(text)) {
            VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(
                    message.getFrom().getId(), vMarkCommand.getCommandIdentifier(), true, VideoWatermarkSettings.class,
                    () -> videoWatermarkStateInitializer.initAndGet(message, vMarkCommand)
            );

            if (videoWatermarkService.isExistsWatermark(message.getFrom().getId())) {
                videoWatermarkSettings.setStateName(watermarkOkState.getName());
                watermarkOkState.enter(message);
            } else {
                videoWatermarkSettings.setStateName(noWatermarkState.getName());
                noWatermarkState.enter(message, true);
            }
            commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
        } else {
            doUpdate(vMarkCommand, message, text);
        }
    }

    protected abstract void doUpdate(VMarkCommand vMarkCommand, Message message, String text);
}

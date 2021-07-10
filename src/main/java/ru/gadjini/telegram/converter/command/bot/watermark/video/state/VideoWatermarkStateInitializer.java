package ru.gadjini.telegram.converter.command.bot.watermark.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;

@Component
@SuppressWarnings("CPD-START")
public class VideoWatermarkStateInitializer {

    private CommandStateService commandStateService;

    private VideoWatermarkService videoWatermarkService;

    private WatermarkOkState watermarkOkState;

    private NoWatermarkState noWatermarkState;

    @Autowired
    public VideoWatermarkStateInitializer(CommandStateService commandStateService,
                                          VideoWatermarkService videoWatermarkService) {
        this.commandStateService = commandStateService;
        this.videoWatermarkService = videoWatermarkService;
    }

    @Autowired
    public void setNoWatermarkState(NoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Autowired
    public void setWatermarkOkState(WatermarkOkState watermarkOkState) {
        this.watermarkOkState = watermarkOkState;
    }

    public VideoWatermarkSettings initAndGet(Message message, VMarkCommand vMarkCommand) {
        VideoWatermarkSettings videoWatermarkSettings = new VideoWatermarkSettings();

        init(message, vMarkCommand, videoWatermarkSettings);

        return videoWatermarkSettings;
    }

    public void init(Message message, VMarkCommand vMarkCommand, VideoWatermarkSettings videoWatermarkSettings) {
        if (videoWatermarkService.isExistsWatermark(message.getFrom().getId())) {
            videoWatermarkSettings.setStateName(watermarkOkState.getName());
        } else {
            videoWatermarkSettings.setStateName(noWatermarkState.getName());
        }
        commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
    }
}

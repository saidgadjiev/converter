package ru.gadjini.telegram.converter.command.bot.watermark.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.NoWatermarkState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.WatermarkOkState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.VideoWatermarkState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;

import java.util.Set;

@Component
public class VMarkCommand implements BotCommand, NavigableBotCommand {

    private NoWatermarkState noWatermarkState;

    private WatermarkOkState watermarkOkState;

    private Set<VideoWatermarkState> watermarkStates;

    private VideoWatermarkService videoWatermarkService;

    private CommandStateService commandStateService;

    @Autowired
    public VMarkCommand(NoWatermarkState noWatermarkState, WatermarkOkState watermarkOkState,
                        Set<VideoWatermarkState> watermarkStates, VideoWatermarkService videoWatermarkService,
                        CommandStateService commandStateService) {
        this.noWatermarkState = noWatermarkState;
        this.watermarkOkState = watermarkOkState;
        this.watermarkStates = watermarkStates;
        this.videoWatermarkService = videoWatermarkService;
        this.commandStateService = commandStateService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        VideoWatermarkSettings videoWatermarkSettings = new VideoWatermarkSettings();

        if (videoWatermarkService.isExistsWatermark(message.getFrom().getId())) {
            videoWatermarkSettings.setStateName(watermarkOkState.getName());
            watermarkOkState.enter(message);
        } else {
            videoWatermarkSettings.setStateName(noWatermarkState.getName());
            noWatermarkState.enter(message);
        }
        commandStateService.setState(message.getChatId(), getCommandIdentifier(), videoWatermarkSettings);
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        VideoWatermarkSettings state = commandStateService.getState(message.getChatId(), getCommandIdentifier(),
                true, VideoWatermarkSettings.class);
        watermarkStates.stream().filter(w -> w.getName().equals(state.getStateName())).findFirst()
                .ifPresent(watermarkState -> watermarkState.update(this, message, text));
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.VMARK;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return getCommandIdentifier();
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getCommandIdentifier());
    }
}

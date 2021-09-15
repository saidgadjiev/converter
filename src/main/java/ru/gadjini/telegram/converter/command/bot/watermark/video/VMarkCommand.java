package ru.gadjini.telegram.converter.command.bot.watermark.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.NoWatermarkState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.VideoWatermarkState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.VideoWatermarkStateInitializer;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.WatermarkOkState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;

import java.util.Locale;
import java.util.Set;

@Component
public class VMarkCommand implements BotCommand, NavigableBotCommand {

    private NoWatermarkState noWatermarkState;

    private WatermarkOkState watermarkOkState;

    private Set<VideoWatermarkState> watermarkStates;

    private VideoWatermarkService videoWatermarkService;

    private CommandStateService commandStateService;

    private ApplicationProperties applicationProperties;

    private LocalisationService localisationService;

    private UserService userService;

    private VideoWatermarkStateInitializer videoWatermarkStateInitializer;

    @Autowired
    public VMarkCommand(Set<VideoWatermarkState> watermarkStates,
                        VideoWatermarkService videoWatermarkService, CommandStateService commandStateService,
                        ApplicationProperties applicationProperties,
                        LocalisationService localisationService, UserService userService,
                        VideoWatermarkStateInitializer videoWatermarkStateInitializer) {
        this.watermarkStates = watermarkStates;
        this.videoWatermarkService = videoWatermarkService;
        this.commandStateService = commandStateService;
        this.applicationProperties = applicationProperties;
        this.localisationService = localisationService;
        this.userService = userService;
        this.videoWatermarkStateInitializer = videoWatermarkStateInitializer;
    }

    @Autowired
    public void setNoWatermarkState(NoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Autowired
    public void setWatermarkOkState(WatermarkOkState watermarkOkState) {
        this.watermarkOkState = watermarkOkState;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        VideoWatermarkSettings videoWatermarkSettings = new VideoWatermarkSettings();

        if (videoWatermarkService.isExistsWatermark(message.getFrom().getId())) {
            videoWatermarkSettings.setStateName(watermarkOkState.getName());
            watermarkOkState.enter(message);
        } else {
            videoWatermarkSettings.setStateName(noWatermarkState.getName());
            noWatermarkState.enter(message, true);
        }
        commandStateService.setState(message.getChatId(), getCommandIdentifier(), videoWatermarkSettings);
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        VideoWatermarkSettings state = commandStateService.getState(message.getChatId(), getCommandIdentifier(),
                true, VideoWatermarkSettings.class,
                () -> {
                    Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
                    if (localisationService.getMessage(ConverterMessagesProperties.CHANGE_WATERMARK_COMMAND_NAME,
                            locale).equals(text)
                            || localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION,
                            locale).equals(text)) {
                        return videoWatermarkStateInitializer.initAndGet(message, this);
                    }

                    return null;
                });
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

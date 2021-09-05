package ru.gadjini.telegram.converter.command.bot.watermark.audio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.settings.AudioWatermarkSettings;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.state.AudioNoWatermarkState;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.state.AudioWatermarkState;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.state.AudioWatermarkStateInitializer;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.state.AudioWatermarkOkState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.service.watermark.audio.AudioWatermarkService;
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
public class AMarkCommand implements BotCommand, NavigableBotCommand {

    private AudioNoWatermarkState noWatermarkState;

    private AudioWatermarkOkState watermarkOkState;

    private Set<AudioWatermarkState> watermarkStates;

    private AudioWatermarkService audioWatermarkService;

    private CommandStateService commandStateService;

    private ApplicationProperties applicationProperties;

    private LocalisationService localisationService;

    private UserService userService;

    private AudioWatermarkStateInitializer audioWatermarkStateInitializer;

    @Autowired
    public AMarkCommand(Set<AudioWatermarkState> watermarkStates,
                        AudioWatermarkService audioWatermarkService, CommandStateService commandStateService,
                        ApplicationProperties applicationProperties,
                        LocalisationService localisationService, UserService userService,
                        AudioWatermarkStateInitializer audioWatermarkStateInitializer) {
        this.watermarkStates = watermarkStates;
        this.audioWatermarkService = audioWatermarkService;
        this.commandStateService = commandStateService;
        this.applicationProperties = applicationProperties;
        this.localisationService = localisationService;
        this.userService = userService;
        this.audioWatermarkStateInitializer = audioWatermarkStateInitializer;
    }

    @Autowired
    public void setNoWatermarkState(AudioNoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Autowired
    public void setWatermarkOkState(AudioWatermarkOkState watermarkOkState) {
        this.watermarkOkState = watermarkOkState;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.AUDIO_CONVERTER);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        AudioWatermarkSettings videoWatermarkSettings = new AudioWatermarkSettings();

        if (audioWatermarkService.isExistsWatermark(message.getFrom().getId())) {
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
        AudioWatermarkSettings state = commandStateService.getState(message.getChatId(), getCommandIdentifier(),
                true, AudioWatermarkSettings.class,
                () -> {
                    Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
                    if (localisationService.getMessage(ConverterMessagesProperties.CHANGE_WATERMARK_COMMAND_NAME,
                            locale).equals(text)
                            || localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION,
                            locale).equals(text)) {
                        return audioWatermarkStateInitializer.initAndGet(message, this);
                    }

                    return null;
                });
        watermarkStates.stream().filter(w -> w.getName().equals(state.getStateName())).findFirst()
                .ifPresent(watermarkState -> watermarkState.update(this, message, text));
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.AMARK;
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

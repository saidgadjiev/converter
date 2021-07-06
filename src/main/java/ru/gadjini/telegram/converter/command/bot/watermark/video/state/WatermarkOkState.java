package ru.gadjini.telegram.converter.command.bot.watermark.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class WatermarkOkState implements VideoWatermarkState {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private NoWatermarkState noWatermarkState;

    private VideoWatermarkService videoWatermarkService;

    @Autowired
    public WatermarkOkState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                            UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                            CommandStateService commandStateService, NoWatermarkState noWatermarkState, VideoWatermarkService videoWatermarkService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.noWatermarkState = noWatermarkState;
        this.videoWatermarkService = videoWatermarkService;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_OK_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.watermarkOkKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    public void watermarkCreatedOrChanged(VMarkCommand vMarkCommand, Message message) {
        VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(), true, VideoWatermarkSettings.class);

        videoWatermarkService.createOrUpdate(videoWatermarkSettings);

        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_CREATED, locale))
                        .replyMarkup(replyKeyboardService.watermarkOkKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void update(VMarkCommand vMarkCommand, Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (localisationService.getMessage(ConverterMessagesProperties.CHANGE_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                    vMarkCommand.getCommandIdentifier(), true, VideoWatermarkSettings.class);

            videoWatermarkSettings.setStateName(noWatermarkState.getName());
            commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
            noWatermarkState.enter(message);
        }
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_OK;
    }
}

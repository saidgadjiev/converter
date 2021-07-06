package ru.gadjini.telegram.converter.command.bot.watermark.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class NoWatermarkState extends BaseWatermarkState {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private WatermarkTextState watermarkTextState;

    private WatermarkImageState watermarkImageState;

    @Autowired
    public NoWatermarkState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                            UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                            CommandStateService commandStateService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setWatermarkTextState(WatermarkTextState state) {
        this.watermarkTextState = state;
    }

    @Autowired
    public void setWatermarkImageState(WatermarkImageState watermarkImageState) {
        this.watermarkImageState = watermarkImageState;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_NO_WATERMARK_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.watermarkTypeKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(VMarkCommand vMarkCommand, Message message, String text) {
        VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(), true, VideoWatermarkSettings.class);

        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (localisationService.getMessage(ConverterMessagesProperties.TEXT_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            videoWatermarkSettings.setWatermarkType(VideoWatermarkType.TEXT);
            videoWatermarkSettings.setStateName(watermarkTextState.getName());
            watermarkTextState.enter(message);
        } else if (localisationService.getMessage(ConverterMessagesProperties.IMAGE_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            videoWatermarkSettings.setWatermarkType(VideoWatermarkType.IMAGE);
            videoWatermarkSettings.setStateName(watermarkImageState.getName());
            watermarkImageState.enter(message);
        } else {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_WATERMARK_TYPE, locale));
        }
        commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.NO_WATERMARK;
    }
}

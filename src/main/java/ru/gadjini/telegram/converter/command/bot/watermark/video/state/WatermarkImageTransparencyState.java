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
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.List;
import java.util.Locale;

@Component
public class WatermarkImageTransparencyState extends BaseWatermarkState {

    private static final List<String> TRANSPARENCIES = List.of("1.0", "0.9", "0.8", "0.7", "0.6", "0.5", "0.4", "0.3",
            "0.2", "0.1");

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private WatermarkPositionState watermarkPositionState;

    @Autowired
    public WatermarkImageTransparencyState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                                           UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                           CommandStateService commandStateService, WatermarkPositionState watermarkPositionState) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.watermarkPositionState = watermarkPositionState;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_IMAGE_TRANSPARENCY_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.watermarkImageTransparencyKeyboard(message.getChatId(), locale, TRANSPARENCIES))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(VMarkCommand vMarkCommand, Message message, String text) {
        VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(),
                true, VideoWatermarkSettings.class);
        videoWatermarkSettings.setTransparency(getTransparency(text, userService.getLocaleOrDefault(message.getFrom().getId())));
        videoWatermarkSettings.setStateName(watermarkPositionState.getName());
        commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
        watermarkPositionState.enter(message, 3);
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_IMAGE_TRANSPARENCY;
    }

    private String getTransparency(String text, Locale locale) {
        if (TRANSPARENCIES.contains(text)) {
            return text;
        }

        throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_IMAGE_INCORRECT_TRANSPARENCY, locale));
    }
}

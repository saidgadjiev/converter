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
public class WatermarkImageSizeState extends BaseWatermarkState {

    public static final String AUTO = "Auto";

    private static final List<String> HEIGHTS = List.of("Auto", "8", "16", "32", "64", "128", "256", "512");

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private WatermarkImageTransparencyState transparencyState;

    @Autowired
    public WatermarkImageSizeState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                                   UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                   CommandStateService commandStateService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setTransparencyState(WatermarkImageTransparencyState transparencyState) {
        this.transparencyState = transparencyState;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_IMAGE_HEIGHT_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.watermarkImageSizeKeyboard(message.getChatId(), locale, HEIGHTS))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(VMarkCommand vMarkCommand, Message message, String text) {
        VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(),
                true, VideoWatermarkSettings.class);
        videoWatermarkSettings.setImageHeight(getHeight(text, userService.getLocaleOrDefault(message.getFrom().getId())));
        videoWatermarkSettings.setStateName(transparencyState.getName());
        commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
        transparencyState.enter(message);
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_IMAGE_SIZE;
    }

    private Integer getHeight(String text, Locale locale) {
        if (HEIGHTS.contains(text)) {
            return text.equals(AUTO) ? null : Integer.parseInt(text);
        }

        throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_WATERMARK_IMAGE_HEIGHT, locale));
    }
}

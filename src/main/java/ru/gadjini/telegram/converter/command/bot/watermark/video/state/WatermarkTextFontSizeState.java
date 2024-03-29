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
public class WatermarkTextFontSizeState extends BaseWatermarkState {

    public static final String AUTO_SIZE = "Auto";

    private static final List<String> FONT_SIZES = List.of("Auto", "20", "24", "28", "32", "36", "40", "46", "50",
            "54", "60", "66");

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private WatermarkColorState watermarkColorState;

    @Autowired
    public WatermarkTextFontSizeState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                                      UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                      CommandStateService commandStateService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setColorState(WatermarkColorState colorState) {
        this.watermarkColorState = colorState;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_TEXT_FONT_SIZE_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.watermarkTextFontSizeKeyboard(message.getChatId(), locale, FONT_SIZES))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(VMarkCommand vMarkCommand, Message message, String text) {
        VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(),
                true, VideoWatermarkSettings.class);
        videoWatermarkSettings.setFontSize(getFontSize(text, userService.getLocaleOrDefault(message.getFrom().getId())));
        videoWatermarkSettings.setStateName(watermarkColorState.getName());
        commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
        watermarkColorState.enter(message);
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_TEXT_FONT_SIZE;
    }

    private Integer getFontSize(String text, Locale locale) {
        if (FONT_SIZES.contains(text)) {
            return text.equals(AUTO_SIZE) ? null : Integer.parseInt(text);
        }

        throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_FONT_SIZE, locale));
    }
}

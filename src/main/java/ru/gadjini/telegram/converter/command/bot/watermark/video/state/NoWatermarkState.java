package ru.gadjini.telegram.converter.command.bot.watermark.video.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.file.WatermarkGifState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.file.WatermarkImageState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.file.WatermarkStickerState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.file.WatermarkVideoState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
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

    private WatermarkGifState watermarkGifState;

    private WatermarkStickerState watermarkStickerState;

    private WatermarkVideoState watermarkVideoState;

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
    public void setWatermarkVideoState(WatermarkVideoState watermarkVideoState) {
        this.watermarkVideoState = watermarkVideoState;
    }

    @Autowired
    public void setWatermarkGifState(WatermarkGifState watermarkGifState) {
        this.watermarkGifState = watermarkGifState;
    }

    @Autowired
    public void setWatermarkStickerState(WatermarkStickerState watermarkStickerState) {
        this.watermarkStickerState = watermarkStickerState;
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
                        .text(localisationService.getCommandWelcomeMessage(ConverterCommandNames.VMARK,
                                ConverterMessagesProperties.MESSAGE_NO_WATERMARK_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.watermarkTypeKeyboard(message.getChatId(), locale, (Boolean) args[0]))
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
        } else if (localisationService.getMessage(ConverterMessagesProperties.STICKER_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            videoWatermarkSettings.setWatermarkType(VideoWatermarkType.STICKER);
            videoWatermarkSettings.setStateName(watermarkStickerState.getName());
            watermarkStickerState.enter(message);
        } else if (localisationService.getMessage(ConverterMessagesProperties.GIF_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            videoWatermarkSettings.setWatermarkType(VideoWatermarkType.GIF);
            videoWatermarkSettings.setStateName(watermarkGifState.getName());
            watermarkGifState.enter(message);
        } else if (localisationService.getMessage(ConverterMessagesProperties.VIDEO_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            videoWatermarkSettings.setWatermarkType(VideoWatermarkType.VIDEO);
            videoWatermarkSettings.setStateName(watermarkVideoState.getName());
            watermarkVideoState.enter(message);
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

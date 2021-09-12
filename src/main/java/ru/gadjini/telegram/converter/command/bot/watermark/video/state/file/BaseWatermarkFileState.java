package ru.gadjini.telegram.converter.command.bot.watermark.video.state.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.BaseWatermarkState;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.WatermarkImageSizeState;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

public abstract class BaseWatermarkFileState extends BaseWatermarkState {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private WatermarkImageSizeState watermarkImageSizeState;

    private MessageMediaService messageMediaService;

    @Autowired
    public void setMessageService(@TgMessageLimitsControl MessageService messageService) {
        this.messageService = messageService;
    }

    @Autowired
    public void setLocalService(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Autowired
    public void setUsService(UserService userService) {
        this.userService = userService;
    }


    @Autowired
    public void setReplyKeyboardService(@KeyboardHolder ConverterReplyKeyboardService replyKeyboardService) {
        this.replyKeyboardService = replyKeyboardService;
    }

    @Autowired
    public void setCommandStService(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setMessageMediaService(MessageMediaService messageMediaService) {
        this.messageMediaService = messageMediaService;
    }

    @Autowired
    public void setWatermarkImageSizeState(WatermarkImageSizeState watermarkImageSizeState) {
        this.watermarkImageSizeState = watermarkImageSizeState;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(getWelcomeMessageCode(), locale))
                        .replyMarkup(replyKeyboardService.watermarkFileKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(VMarkCommand vMarkCommand, Message message, String text) {
        VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(),
                true, VideoWatermarkSettings.class);
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        validate(media, locale);

        videoWatermarkSettings.setImage(media);
        videoWatermarkSettings.setStateName(watermarkImageSizeState.getName());
        commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
        watermarkImageSizeState.enter(message);
    }

    protected abstract String getWelcomeMessageCode();

    protected abstract void validate(MessageMedia messageMedia, Locale locale);
}

package ru.gadjini.telegram.converter.command.bot.watermark.audio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.AMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.settings.AudioWatermarkSettings;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.Objects;

@Component
public class AudioNoWatermarkState extends BaseAudioWatermarkState {

    private MessageService messageService;

    private LocalisationService localisationService;

    private MessageMediaService messageMediaService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private AudioWatermarkOkState watermarkOkState;

    @Autowired
    public AudioNoWatermarkState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                                 MessageMediaService messageMediaService, UserService userService,
                                 @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                 CommandStateService commandStateService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.messageMediaService = messageMediaService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setWatermarkRootState(AudioWatermarkOkState watermarkOkState) {
        this.watermarkOkState = watermarkOkState;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_NO_WATERMARK_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.cancelKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(AMarkCommand aMarkCommand, Message message, String text) {
        AudioWatermarkSettings audioWatermarkSettings = commandStateService.getState(message.getChatId(),
                aMarkCommand.getCommandIdentifier(), true, AudioWatermarkSettings.class);

        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        MessageMedia messageMedia = messageMediaService.getMedia(message, locale);
        if (messageMedia == null || !Objects.equals(messageMedia.getFormat() == null ? null : messageMedia.getFormat().getCategory(),
                FormatCategory.AUDIO)) {
            throw new UserException(localisationService.getMessage(
                    ConverterMessagesProperties.MESSAGE_AUDIO_NO_WATERMARK_AWAITING_AUDIO, locale));
        }
        audioWatermarkSettings.setAudio(messageMedia);
        audioWatermarkSettings.setStateName(watermarkOkState.getName());
        commandStateService.setState(message.getChatId(), aMarkCommand.getCommandIdentifier(), audioWatermarkSettings);
        watermarkOkState.watermarkCreatedOrChanged(aMarkCommand, message);
    }

    @Override
    public AudioWatermarkStateName getName() {
        return AudioWatermarkStateName.NO_WATERMARK;
    }
}

package ru.gadjini.telegram.converter.command.bot.watermark.audio.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.AMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.settings.AudioWatermarkSettings;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.watermark.audio.AudioWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.job.WorkQueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class AudioWatermarkOkState extends BaseAudioWatermarkState {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private AudioNoWatermarkState noWatermarkState;

    private AudioWatermarkService audioWatermarkService;

    private AudioWatermarkStateInitializer stateInitializer;

    private MessageMediaService messageMediaService;

    private ConvertionService convertionService;

    private WorkQueueJob workQueueJob;

    @Autowired
    public AudioWatermarkOkState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                                 UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                 CommandStateService commandStateService,
                                 AudioWatermarkService audioWatermarkService,
                                 MessageMediaService messageMediaService,
                                 ConvertionService convertionService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.audioWatermarkService = audioWatermarkService;
        this.messageMediaService = messageMediaService;
        this.convertionService = convertionService;
    }

    @Autowired
    public void setStateInitializer(AudioWatermarkStateInitializer audioWatermarkStateInitializer) {
        this.stateInitializer = audioWatermarkStateInitializer;
    }

    @Autowired
    public void setWorkQueueJob(WorkQueueJob workQueueJob) {
        this.workQueueJob = workQueueJob;
    }

    @Autowired
    public void setNoWState(AudioNoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Override
    public void enter(Message message, Object... args) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_WATERMARK_OK_WELCOME, locale) + "\n\n" +
                                buildWatermarkInfo(locale))
                        .replyMarkup(replyKeyboardService.watermarkOkKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    public void watermarkCreatedOrChanged(AMarkCommand vMarkCommand, Message message) {
        AudioWatermarkSettings audioWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(), true, AudioWatermarkSettings.class);

        audioWatermarkService.createOrUpdate(message.getFrom().getId(), audioWatermarkSettings);

        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_WATERMARK_CREATED, locale) + "\n\n" +
                                buildWatermarkInfo(locale))
                        .replyMarkup(replyKeyboardService.watermarkOkKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(AMarkCommand aMarkCommand, Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (localisationService.getMessage(ConverterMessagesProperties.CHANGE_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            AudioWatermarkSettings audioWatermarkSettings = commandStateService.getState(message.getChatId(),
                    aMarkCommand.getCommandIdentifier(), true, AudioWatermarkSettings.class,
                    () -> stateInitializer.initAndGet(message, aMarkCommand));

            audioWatermarkSettings.setStateName(noWatermarkState.getName());
            commandStateService.setState(message.getChatId(), aMarkCommand.getCommandIdentifier(), audioWatermarkSettings);
            noWatermarkState.enter(message, false);
        } else {
            ConvertState convertState = createState(message, locale);

            workQueueJob.cancelCurrentTasks(message.getChatId());
            convertionService.createConversion(message.getFrom(), convertState, Format.WATERMARK, locale);
        }
    }

    @Override
    public AudioWatermarkStateName getName() {
        return AudioWatermarkStateName.WATERMARK_OK;
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());

        MessageMedia media = messageMediaService.getMedia(message, locale);
        if (media != null) {
            checkAudioFormat(media.getFormat(), locale);

            convertState.addMedia(media);
        } else {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_AUDIO_FILE, locale));
        }

        return convertState;
    }

    private void checkAudioFormat(Format format, Locale locale) {
        if (format == null || format.getCategory() != FormatCategory.AUDIO) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_AUDIO_FILE, locale));
        }
    }

    private String buildWatermarkInfo(Locale locale) {
        return localisationService.getCommandWelcomeMessage(ConverterCommandNames.AMARK, ConverterMessagesProperties.MESSAGE_AUDIO_WATERMARK, locale);
    }
}

package ru.gadjini.telegram.converter.command.bot.watermark.video.state;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
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
public class WatermarkOkState extends BaseWatermarkState {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private NoWatermarkState noWatermarkState;

    private VideoWatermarkService videoWatermarkService;

    private VideoWatermarkStateInitializer stateInitializer;

    private MessageMediaService messageMediaService;

    private ConvertionService convertionService;

    private WorkQueueJob workQueueJob;

    @Autowired
    public WatermarkOkState(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                            UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                            CommandStateService commandStateService,
                            VideoWatermarkService videoWatermarkService,
                            MessageMediaService messageMediaService,
                            ConvertionService convertionService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.videoWatermarkService = videoWatermarkService;
        this.messageMediaService = messageMediaService;
        this.convertionService = convertionService;
    }

    @Autowired
    public void setStateInitializer(VideoWatermarkStateInitializer videoWatermarkStateInitializer) {
        this.stateInitializer = videoWatermarkStateInitializer;
    }

    @Autowired
    public void setWorkQueueJob(WorkQueueJob workQueueJob) {
        this.workQueueJob = workQueueJob;
    }

    @Autowired
    public void setNoWState(NoWatermarkState noWatermarkState) {
        this.noWatermarkState = noWatermarkState;
    }

    @Override
    public void enter(Message message, Object... args) {
        VideoWatermark videoWatermark = videoWatermarkService.getWatermark(message.getFrom().getId());
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_OK_WELCOME, locale) + "\n\n" +
                                buildWatermarkInfo(videoWatermark, locale))
                        .replyMarkup(replyKeyboardService.watermarkOkKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    public void watermarkCreatedOrChanged(VMarkCommand vMarkCommand, Message message) {
        VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                vMarkCommand.getCommandIdentifier(), true, VideoWatermarkSettings.class);

        videoWatermarkService.createOrUpdate(message.getFrom().getId(), videoWatermarkSettings);

        VideoWatermark videoWatermark = videoWatermarkService.getWatermark(message.getFrom().getId());
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_CREATED, locale) + "\n\n" +
                                buildWatermarkInfo(videoWatermark, locale))
                        .replyMarkup(replyKeyboardService.watermarkOkKeyboard(message.getChatId(), locale))
                        .parseMode(ParseMode.HTML)
                        .build()
        );
    }

    @Override
    public void doUpdate(VMarkCommand vMarkCommand, Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (localisationService.getMessage(ConverterMessagesProperties.CHANGE_WATERMARK_COMMAND_NAME, locale).equals(text)) {
            VideoWatermarkSettings videoWatermarkSettings = commandStateService.getState(message.getChatId(),
                    vMarkCommand.getCommandIdentifier(), true, VideoWatermarkSettings.class,
                    () -> stateInitializer.initAndGet(message, vMarkCommand));

            videoWatermarkSettings.setStateName(noWatermarkState.getName());
            commandStateService.setState(message.getChatId(), vMarkCommand.getCommandIdentifier(), videoWatermarkSettings);
            noWatermarkState.enter(message, false);
        } else {
            ConvertState convertState = createState(message, locale);

            workQueueJob.cancelCurrentTasks(message.getChatId());
            convertionService.createConversion(message.getFrom(), convertState, Format.WATERMARK, locale);
        }
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_OK;
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());

        MessageMedia media = messageMediaService.getMedia(message, locale);
        if (media != null) {
            checkVideoFormat(media.getFormat(), locale);

            convertState.addMedia(media);
        } else {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_VIDEO_FILE, locale));
        }

        return convertState;
    }

    private void checkVideoFormat(Format format, Locale locale) {
        if (format == null || format.getCategory() != FormatCategory.VIDEO) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_VIDEO_FILE, locale));
        }
    }

    private String buildWatermarkInfo(VideoWatermark videoWatermark, Locale locale) {
        if (videoWatermark.getWatermarkType() == VideoWatermarkType.TEXT) {
            String text = StringUtils.substring(videoWatermark.getText(), 0, 128);
            String positionMessageCode = videoWatermark.getWatermarkPosition().name().toLowerCase().replace("_", ".");
            return localisationService.getCommandWelcomeMessage(
                    ConverterCommandNames.VMARK,
                    ConverterMessagesProperties.MESSAGE_TEXT_WATERMARK,
                    new Object[]{
                            text + (videoWatermark.getText().length() > 128 ? "..." : ""),
                            videoWatermark.getFontSize() == null ? WatermarkTextFontSizeState.AUTO_SIZE : videoWatermark.getFontSize(),
                            videoWatermark.getColor().name().toLowerCase(),
                            localisationService.getMessage(positionMessageCode, locale)
                    },
                    locale
            );
        } else {
            String positionMessageCode = videoWatermark.getWatermarkPosition().name().toLowerCase().replace("_", ".");
            return localisationService.getCommandWelcomeMessage(
                    ConverterCommandNames.VMARK,
                    ConverterMessagesProperties.MESSAGE_IMAGE_WATERMARK,
                    new Object[]{
                            videoWatermark.getWatermarkType().name(),
                            videoWatermark.getImageHeight() == null ? WatermarkImageSizeState.AUTO : videoWatermark.getImageHeight(),
                            videoWatermark.getTransparency(),
                            localisationService.getMessage(positionMessageCode, locale)
                    },
                    locale
            );
        }
    }
}

package ru.gadjini.telegram.converter.command.keyboard;

import com.antkorwin.xsync.XSync;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.converter.service.conversion.impl.ConvertState;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.job.WorkQueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.Objects;

@Component
public class StartCommand implements NavigableBotCommand, BotCommand {

    public static final String TAG = "convert";

    private static final Logger LOGGER = LoggerFactory.getLogger(StartCommand.class);

    private CommandStateService commandStateService;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private FormatService formatService;

    private ConvertionService conversionService;

    private ConversionMessageBuilder queueMessageBuilder;

    private MessageMediaService messageMediaService;

    private ConversionFormatService conversionFormatService;

    private XSync<Long> longXSync;

    private WorkQueueJob conversionJob;

    @Autowired
    public StartCommand(CommandStateService commandStateService, UserService userService,
                        @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService,
                        @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService,
                        FormatService formatService, ConvertionService convertionService,
                        ConversionMessageBuilder queueMessageBuilder, MessageMediaService messageMediaService,
                        ConversionFormatService conversionFormatService, XSync<Long> longXSync, WorkQueueJob conversionJob) {
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.formatService = formatService;
        this.conversionService = convertionService;
        this.queueMessageBuilder = queueMessageBuilder;
        this.messageMediaService = messageMediaService;
        this.conversionFormatService = conversionFormatService;
        this.longXSync = longXSync;
        this.conversionJob = conversionJob;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        longXSync.execute(message.getChatId(), () -> {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

            ConvertState convertState = commandStateService.getState(message.getChatId(), ConverterCommandNames.START_COMMAND, false, ConvertState.class);
            if (convertState == null) {
                check(message, locale);
                convertState = createState(message, locale);
                if (StringUtils.isNotBlank(message.getMediaGroupId())) {
                    messageService.sendMessage(
                            SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                    .text(queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                                    .replyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), Format.IMAGES, locale))
                                    .parseMode(ParseMode.HTML)
                                    .build()
                    );
                } else {
                    messageService.sendMessage(
                            SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                    .text(queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                                    .parseMode(ParseMode.HTML)
                                    .replyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFirstFormat(), locale))
                                    .build()
                    );
                }
                convertState.deleteWarns();
                commandStateService.setState(message.getChatId(), ConverterCommandNames.START_COMMAND, convertState);
            } else if (isMediaMessage(message)) {
                MessageMedia media = messageMediaService.getMedia(message, locale);
                if (isMultiImageMessage(media, convertState)) {
                    convertState.addMedia(media);
                    if (StringUtils.isNotBlank(message.getMediaGroupId()) && !Objects.equals(convertState.getMediaGroupId(), message.getMediaGroupId())) {
                        convertState.setMediaGroupId(message.getMediaGroupId());
                    }
                    if (convertState.getFiles().size() < 3) {
                        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(localisationService.getMessage(MessagesProperties.MESSAGE_FILES_APPENDED, locale))
                                .replyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), Format.IMAGES, locale)).build());
                    }
                    commandStateService.setState(message.getChatId(), ConverterCommandNames.START_COMMAND, convertState);
                } else {
                    messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                            .text(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_TYPE, locale))
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFirstFormat(), locale))
                            .build());
                }
            } else if (message.hasText()) {
                Format associatedFormat = formatService.getAssociatedFormat(text);

                if (isMultiTextMessage(associatedFormat, convertState)) {
                    convertState.getFirstFile().setFileId(convertState.getFirstFile().getFileId() + " " + text);
                    if (!convertState.isTextAppendedMessageSent()) {
                        convertState.setTextAppendedMessageSent(true);
                        commandStateService.setState(message.getChatId(), ConverterCommandNames.START_COMMAND, convertState);

                        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(localisationService.getMessage(MessagesProperties.MESSAGE_TEXT_APPENDED, locale))
                                .replyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFirstFormat(), locale))
                                .build());
                    } else {
                        commandStateService.setState(message.getChatId(), ConverterCommandNames.START_COMMAND, convertState);
                    }
                    return;
                }
                Format targetFormat = checkTargetFormat(message.getFrom().getId(), convertState.getFirstFormat(), associatedFormat, text, locale);
                conversionJob.removeAndCancelCurrentTasks(message.getFrom().getId());

                conversionService.createConversion(message.getFrom(), convertState, targetFormat, locale);
                commandStateService.deleteState(message.getChatId(), ConverterCommandNames.START_COMMAND);
            } else {
                messageService.sendMessage(
                        SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                                .replyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFirstFormat(), locale))
                                .parseMode(ParseMode.HTML)
                                .build()
                );
            }
        });
    }

    @Override
    public String getParentCommandName(long chatId) {
        return ConverterCommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return ConverterCommandNames.START_COMMAND;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId(), getHistoryName());
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                .text(queueMessageBuilder.getWelcomeMessage(locale))
                .replyMarkup(replyKeyboardService.removeKeyboard(message.getChatId()))
                .parseMode(ParseMode.HTML).build());
    }

    @Override
    public ReplyKeyboard getKeyboard(long chatId) {
        return replyKeyboardService.removeKeyboard(chatId);
    }

    @Override
    public String getMessage(long chatId) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);

        return queueMessageBuilder.getWelcomeMessage(locale);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        ConvertState convertState = commandStateService.getState(message.getChatId(), ConverterCommandNames.START_COMMAND, false, ConvertState.class);
        if (convertState == null) {
            messageService.sendMessage(
                    SendMessage.builder().chatId(String.valueOf(message.getChatId())).text(queueMessageBuilder.getWelcomeMessage(locale))
                            .replyMarkup(replyKeyboardService.removeKeyboard(message.getChatId()))
                            .parseMode(ParseMode.HTML)
                            .build()
            );
        } else {
            messageService.sendMessage(
                    SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                            .text(queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                            .replyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFirstFormat(), locale))
                            .parseMode(ParseMode.HTML)
                            .build()
            );
        }
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.START_COMMAND;
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
    }

    @Override
    public boolean canLeave(long chatId) {
        if (commandStateService.hasState(chatId, getHistoryName())) {
            commandStateService.deleteState(chatId, getHistoryName());

            return false;
        }

        return true;
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setMediaGroupId(message.getMediaGroupId());

        LOGGER.debug("Convert state({}, {})", message.getChatId(), TgMessage.getMetaTypes(message));

        MessageMedia media = messageMediaService.getMedia(message, locale);
        if (media != null) {
            checkFormat(message.getFrom().getId(), media.getFormat(), media.getMimeType(), media.getFileName(), locale);

            convertState.addMedia(media);
        } else if (message.hasText()) {
            MessageMedia messageMedia = new MessageMedia();
            messageMedia.setFileId(message.getText());
            messageMedia.setFileSize(message.getText().length());
            messageMedia.setFormat(formatService.getFormat(message.getText()));
            checkFormat(message.getFrom().getId(), messageMedia.getFormat(), null, null, locale);

            convertState.addMedia(messageMedia);
        } else {
            throw new UserException(queueMessageBuilder.getWelcomeMessage(locale));
        }

        return convertState;
    }

    private void check(Message message, Locale locale) {
        if (message.hasDocument() || message.hasText() || message.hasPhoto()
                || message.hasSticker() || message.hasVideo() || message.hasAudio() || message.hasVoice()) {
            return;
        }

        LOGGER.warn("Unsupported format({}, {})", message.getChatId(), TgMessage.getMetaTypes(message));
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
    }

    private Format checkFormat(int userId, Format format, String mimeType, String fileName, Locale locale) {
        if (format == null) {
            LOGGER.warn("Format is null({}, {}, {})", userId, mimeType, fileName);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
        }
        if (format.getCategory() == FormatCategory.ARCHIVE) {
            LOGGER.warn("Archive unsupported({})", userId);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FILE, new Object[]{mimeType}, locale));
        }
        if (!conversionFormatService.isSupportedCategory(format.getCategory())) {
            LOGGER.warn("Category unsupported({}, {})", userId, format.getCategory());
            throw new UserException(queueMessageBuilder.getUnsupportedCategoryMessage(format.getCategory(), locale));
        }

        return format;
    }

    private Format checkTargetFormat(int userId, Format format, Format target, String text, Locale locale) {
        if (target == null) {
            LOGGER.warn("Target format is null({}, {})", userId, text);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
        }
        if (Objects.equals(format, target)) {
            LOGGER.warn("Same formats({}, {})", userId, format);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_ANOTHER_FORMAT, locale));
        }
        boolean result = conversionFormatService.isConvertAvailable(format, target);
        if (result) {
            return target;
        }

        LOGGER.warn("Conversion unavailable({}, {}, {})", userId, format, target);
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
    }

    private boolean isMultiTextMessage(Format associatedFormat, ConvertState convertState) {
        return associatedFormat == null && convertState.getFirstFormat() == Format.TEXT;
    }

    private boolean isMultiImageMessage(MessageMedia media, ConvertState convertState) {
        return media != null
                && media.getFormat() != null
                && media.getFormat().getCategory() == FormatCategory.IMAGES
                && convertState.getFiles().stream().allMatch(m -> m.getFormat() != null && m.getFormat().getCategory() == FormatCategory.IMAGES);
    }

    private boolean isMediaMessage(Message message) {
        return message.hasDocument() || message.hasPhoto() || message.hasVideo();
    }
}

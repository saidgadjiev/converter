package ru.gadjini.telegram.converter.command.keyboard;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.converter.service.conversion.impl.ConvertState;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private TempFileService fileService;

    private FileManager fileManager;

    private MessageMediaService messageMediaService;

    private ConversionFormatService conversionFormatService;

    @Autowired
    public StartCommand(CommandStateService commandStateService, UserService userService,
                        @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService,
                        @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService,
                        FormatService formatService, ConvertionService convertionService,
                        ConversionMessageBuilder queueMessageBuilder,
                        TempFileService fileService, FileManager fileManager, MessageMediaService messageMediaService,
                        ConversionFormatService conversionFormatService) {
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.formatService = formatService;
        this.conversionService = convertionService;
        this.queueMessageBuilder = queueMessageBuilder;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.messageMediaService = messageMediaService;
        this.conversionFormatService = conversionFormatService;
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        ConvertState convertState = commandStateService.getState(message.getChatId(), CommandNames.START_COMMAND, false, ConvertState.class);
        if (convertState == null) {
            check(message, locale);
            convertState = createState(message, locale);
            messageService.sendMessage(
                    new HtmlMessage(message.getChatId(), queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                            .setReplyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFormat(), locale))
            );
            convertState.deleteWarns();
            commandStateService.setState(message.getChatId(), CommandNames.START_COMMAND, convertState);
        } else if (isMediaMessage(message)) {
            messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_TOO_MANY_FILES, locale))
                    .setReplyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFormat(), locale)));
        } else if (message.hasText()) {
            Format associatedFormat = formatService.getAssociatedFormat(text);

            if (isMultiTextMessage(associatedFormat, convertState)) {
                convertState.setFileId(convertState.getFileId() + " " + text);
                commandStateService.setState(message.getChatId(), CommandNames.START_COMMAND, convertState);
                messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_TEXT_APPENDED, locale))
                        .setReplyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFormat(), locale)));
                return;
            }
            Format targetFormat = checkTargetFormat(message.getFrom().getId(), convertState.getFormat(), associatedFormat, text, locale);
            if (targetFormat == Format.GIF) {
                convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_GIF_WARN, locale));
            }
            conversionService.convert(message.getFrom(), convertState, targetFormat, locale);
            commandStateService.deleteState(message.getChatId(), CommandNames.START_COMMAND);
        } else {
            messageService.sendMessage(
                    new HtmlMessage(message.getChatId(), queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                            .setReplyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFormat(), locale))
            );
        }
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId(), getHistoryName());
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                .setReplyMarkup(replyKeyboardService.removeKeyboard(message.getChatId())));
    }

    @Override
    public ReplyKeyboard getKeyboard(long chatId) {
        return replyKeyboardService.removeKeyboard(chatId);
    }

    @Override
    public String getMessage(long chatId) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);

        return localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        ConvertState convertState = commandStateService.getState(message.getChatId(), CommandNames.START_COMMAND, false, ConvertState.class);
        if (convertState == null) {
            messageService.sendMessage(
                    new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                            .setReplyMarkup(replyKeyboardService.removeKeyboard(message.getChatId()))
            );
        } else {
            messageService.sendMessage(
                    new HtmlMessage(message.getChatId(), queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                            .setReplyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFormat(), locale))
            );
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.START_COMMAND;
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

        LOGGER.debug("Convert state({}, {})", message.getChatId(), TgMessage.getMetaTypes(message));

        MessageMedia media = messageMediaService.getMedia(message, locale);
        if (media != null) {
            checkFormat(message.getFrom().getId(), media.getFormat(), media.getMimeType(), media.getFileName(), locale);
            if (media.getFormat() == Format.HTML && isBaseUrlMissed(message.getChatId(), media.getFileId(), media.getFileSize())) {
                convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_NO_BASE_URL_IN_HTML, locale));
            }
            convertState.setMedia(media);
        } else if (message.hasText()) {
            convertState.setFileId(message.getText());
            convertState.setFileSize((long) message.getText().length());
            convertState.setFormat(formatService.getFormat(message.getText()));
        } else {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale));
        }

        return convertState;
    }

    private boolean isBaseUrlMissed(long chatId, String fileId, long fileSize) {
        SmartTempFile file = fileService.createTempFile(chatId, fileId, TAG, Format.HTML.getExt());

        try {
            fileManager.downloadFileByFileId(fileId, fileSize, file);

            Document parse = Jsoup.parse(file.getFile(), StandardCharsets.UTF_8.name());
            Elements base = parse.head().getElementsByTag("base");

            return base == null || base.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            file.smartDelete();
        }
    }

    private void check(Message message, Locale locale) {
        if (message.hasDocument() || message.hasText() || message.hasPhoto()
                || message.hasSticker() || message.hasVideo()) {
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
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
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
        return associatedFormat == null && convertState.getFormat() == Format.TEXT;
    }

    private boolean isMediaMessage(Message message) {
        return message.hasDocument() || message.hasPhoto() || message.hasVideo();
    }
}

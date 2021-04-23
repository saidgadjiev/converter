package ru.gadjini.telegram.converter.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static ru.gadjini.telegram.converter.service.conversion.impl.VaiMakeConverter.AUDIO_FILE_INDEX;
import static ru.gadjini.telegram.converter.service.conversion.impl.VaiMakeConverter.IMAGE_FILE_INDEX;

@Component
public class VaiMakeCommand implements NavigableBotCommand, BotCommand {

    private static final Set<FormatCategory> ACCEPT_CATEGORIES = Set.of(FormatCategory.AUDIO, FormatCategory.IMAGES);

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private MessageMediaService messageMediaService;

    private ConvertionService convertionService;

    private ApplicationProperties applicationProperties;

    @Autowired
    public VaiMakeCommand(@TgMessageLimitsControl MessageService messageService,
                          LocalisationService localisationService, UserService userService,
                          @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                          CommandStateService commandStateService, MessageMediaService messageMediaService,
                          ConvertionService convertionService, ApplicationProperties applicationProperties) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.messageMediaService = messageMediaService;
        this.convertionService = convertionService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.vmakeKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        ConvertState existsState = commandStateService.getState(message.getChatId(), getCommandIdentifier(), false, ConvertState.class);

        if (existsState == null) {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            ConvertState convertState = createState(message, locale);
            messageService.sendMessage(
                    SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                            .text(getAwaitingMessage(convertState))
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(replyKeyboardService.vmakeKeyboard(message.getChatId(), locale))
                            .build()
            );
            commandStateService.setState(message.getChatId(), getCommandIdentifier(), convertState);
        } else {
            Locale locale = new Locale(existsState.getUserLanguage());
            MessageMedia media = messageMediaService.getMedia(message, locale);
            if (media != null) {
                checkSecondMedia(existsState, media);
                existsState.setMedia(getIndex(media), media);

                messageService.sendMessage(
                        SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_CLICK, locale))
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(replyKeyboardService.vmakeKeyboard(message.getChatId(), locale))
                                .build()
                );
                commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
            } else if (message.hasText()) {
                String cancelFilesCommand = localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale);
                String vmakeCommand = localisationService.getMessage(ConverterMessagesProperties.VMAKE_COMMAND_NAME, locale);
                if (Objects.equals(text, cancelFilesCommand)) {
                    messageService.sendMessage(
                            SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_FILES_CANCELED, locale))
                                    .parseMode(ParseMode.HTML)
                                    .replyMarkup(replyKeyboardService.vmakeKeyboard(message.getChatId(), locale))
                                    .build()
                    );
                    commandStateService.deleteState(message.getChatId(), getCommandIdentifier());
                } else if (Objects.equals(text, vmakeCommand)) {
                    validateVaiMake(existsState);
                    convertionService.createConversion(message.getFrom(), existsState, Format.VMAKE, locale);
                    commandStateService.deleteState(message.getChatId(), getCommandIdentifier());
                }
            }
        }
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.VAIMAKE;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return getCommandIdentifier();
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getCommandIdentifier());
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState(2);
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        convertState.setMedia(getIndex(media), media);

        return convertState;
    }

    private int getIndex(MessageMedia media) {
        return media.getFormat().getCategory() == FormatCategory.AUDIO ? AUDIO_FILE_INDEX : IMAGE_FILE_INDEX;
    }

    private void validateVaiMake(ConvertState convertState) {
        if (convertState.getMedia(AUDIO_FILE_INDEX) != null && convertState.getMedia(IMAGE_FILE_INDEX) != null) {
            return;
        }

        throw new UserException(getAwaitingMessage(convertState));
    }

    private String getAwaitingMessage(ConvertState convertState) {
        if (convertState.getMedia(AUDIO_FILE_INDEX) != null) {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_AWAITING_IMAGE, new Locale(convertState.getUserLanguage()));
        } else {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_AWAITING_AUDIO, new Locale(convertState.getUserLanguage()));
        }
    }

    private void checkSecondMedia(ConvertState convertState, MessageMedia media) {
        if (convertState.getMedia(AUDIO_FILE_INDEX) != null) {
            if (media == null || media.getFormat().getCategory() != FormatCategory.IMAGES) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_AWAITING_IMAGE,
                        new Locale(convertState.getUserLanguage())));
            }
        } else {
            if (media == null || media.getFormat().getCategory() != FormatCategory.AUDIO) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_AWAITING_AUDIO,
                        new Locale(convertState.getUserLanguage())));
            }
        }
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || !ACCEPT_CATEGORIES.contains(media.getFormat().getCategory())) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_WELCOME, locale));
        }
    }
}

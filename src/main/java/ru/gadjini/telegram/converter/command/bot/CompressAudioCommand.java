package ru.gadjini.telegram.converter.command.bot;

import com.antkorwin.xsync.XSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.conversion.impl.FFmpegAudioCompressConverter;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
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
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Component
public class CompressAudioCommand implements BotCommand, NavigableBotCommand, CallbackBotCommand {

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private ConvertionService convertionService;

    private WorkQueueJob workQueueJob;

    private MessageMediaService messageMediaService;

    private XSync<Long> longXSync;

    @Autowired
    public CompressAudioCommand(@Qualifier("messageLimits") MessageService messageService, UserService userService,
                                LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                                @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService,
                                CommandStateService commandStateService, ConvertionService convertionService,
                                WorkQueueJob workQueueJob, MessageMediaService messageMediaService,
                                XSync<Long> longXSync) {
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.convertionService = convertionService;
        this.workQueueJob = workQueueJob;
        this.messageMediaService = messageMediaService;
        this.longXSync = longXSync;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {

    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        messageService.sendMessage(
                SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESS_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.getAudioCompressionKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public String getName() {
        return ConverterCommandNames.COMPRESS_AUDIO;
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.COMPRESS_AUDIO;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return ConverterCommandNames.COMPRESS_AUDIO;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        longXSync.execute(message.getChatId(), () -> {
            if (message.hasText()) {
                setBitrate(message.getChatId(), text);
            } else {
                ConvertState existsState = commandStateService.getState(message.getChatId(), ConverterCommandNames.COMPRESS_AUDIO, false, ConvertState.class);

                if (existsState == null) {
                    Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
                    ConvertState convertState = createState(message, locale);
                    messageService.sendMessage(
                            SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_SETTINGS, new Object[]{
                                            FFmpegAudioCompressConverter.AUTO_BITRATE
                                    }, locale) + "\n\n" + localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_AUTO_BITRATE, locale))
                                    .parseMode(ParseMode.HTML)
                                    .replyMarkup(inlineKeyboardService.getAudioCompressionSettingsKeyboard(locale))
                                    .build(),
                            sent -> {
                                convertState.getSettings().setMessageId(sent.getMessageId());
                                commandStateService.setState(sent.getChatId(), getCommandIdentifier(), convertState);
                            }
                    );
                } else {
                    updateState(existsState, message);
                    commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
                }
            }
        });
    }

    @Override
    public void processNonCommandCallback(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.AUTO_BIT_RATE.getKey())) {
            setAutoBitrate(callbackQuery.getMessage().getChatId(), callbackQuery.getId());
        } else if (requestParams.contains(ConverterArg.COMPRESS.getKey())) {
            ConvertState convertState = commandStateService.getState(callbackQuery.getMessage().getChatId(), ConverterCommandNames.COMPRESS_AUDIO, false, ConvertState.class);

            if (convertState != null) {
                workQueueJob.removeAndCancelCurrentTasks(callbackQuery.getMessage().getChatId());
                convertionService.createConversion(callbackQuery.getFrom(), convertState, Format.COMPRESS, new Locale(convertState.getUserLanguage()));
                commandStateService.deleteState(callbackQuery.getMessage().getChatId(), ConverterCommandNames.COMPRESS_AUDIO);
                messageService.removeInlineKeyboard(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
            }
        }
    }

    @Override
    public void leave(long chatId) {
        ConvertState state = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, false, ConvertState.class);
        if (state != null) {
            messageService.removeInlineKeyboard(chatId, state.getSettings().getMessageId());
        }
        commandStateService.deleteState(chatId, ConverterCommandNames.COMPRESS_AUDIO);
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());
        convertState.getSettings().setBitrate(FFmpegAudioCompressConverter.AUTO_BITRATE);
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        convertState.setMedia(media);

        return convertState;
    }

    private void updateState(ConvertState convertState, Message message) {
        MessageMedia media = messageMediaService.getMedia(message, new Locale(convertState.getUserLanguage()));

        if (media == null || media.getFormat().getCategory() != FormatCategory.AUDIO) {
            return;
        }
        convertState.setMedia(media);
    }

    private void setAutoBitrate(long chatId, String queryId) {
        setBitrate(chatId, queryId, FFmpegAudioCompressConverter.AUTO_BITRATE);
    }

    private void setBitrate(long chatId, String bitrate) {
        setBitrate(chatId, null, bitrate);
    }

    private void setBitrate(long chatId, String queryId, String bitrate) {
        ConvertState convertState = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);
        Locale locale = new Locale(convertState.getUserLanguage());
        String messageText;
        if (FFmpegAudioCompressConverter.AUTO_BITRATE.equals(bitrate)) {
            if (FFmpegAudioCompressConverter.AUTO_BITRATE.equals(convertState.getSettings().getBitrate())) {
                messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder().callbackQueryId(queryId)
                        .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_AUTO_BITRATE_CHOSE,
                                locale)).build());
                return;
            } else {
                convertState.getSettings().setBitrate(bitrate);
                messageText = localisationService.getMessage(
                        MessagesProperties.MESSAGE_AUDIO_COMPRESSION_SETTINGS, new Object[]{convertState.getSettings().getBitrate()}, locale
                ) + "\n\n" + localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_AUTO_BITRATE, locale);
            }
        } else {
            //TODO: validate
            convertState.getSettings().setBitrate(bitrate);
            messageText = localisationService.getMessage(
                    MessagesProperties.MESSAGE_AUDIO_COMPRESSION_SETTINGS, new Object[]{convertState.getSettings().getBitrate() + "k"}, locale
            );
        }
        messageService.editMessage(EditMessageText.builder().chatId(String.valueOf(chatId))
                .messageId(convertState.getSettings().getMessageId())
                .text(messageText)
                .parseMode(ParseMode.HTML)
                .replyMarkup(inlineKeyboardService.getAudioCompressionSettingsKeyboard(locale))
                .build());
        commandStateService.setState(chatId, ConverterCommandNames.COMPRESS_AUDIO, convertState);
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat().getCategory() != FormatCategory.AUDIO) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESS_FILE_NOT_FOUND, locale));
        }
    }
}

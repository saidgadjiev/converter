package ru.gadjini.telegram.converter.command.bot;

import com.antkorwin.xsync.XSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.AudioCompressionMode;
import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.state.StateFatherAudioCompressSettingsState;
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

    private StateFatherAudioCompressSettingsState stateFather;

    private XSync<Long> longXSync;

    @Autowired
    public CompressAudioCommand(@Qualifier("messageLimits") MessageService messageService, UserService userService,
                                LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                                @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService,
                                CommandStateService commandStateService, ConvertionService convertionService,
                                WorkQueueJob workQueueJob, MessageMediaService messageMediaService,
                                StateFatherAudioCompressSettingsState stateFather, XSync<Long> longXSync) {
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.convertionService = convertionService;
        this.workQueueJob = workQueueJob;
        this.messageMediaService = messageMediaService;
        this.stateFather = stateFather;
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
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            if (message.hasText()) {
                String compressCommand = localisationService.getMessage(MessagesProperties.AUDIO_COMPRESSION_COMPRESS_COMMAND_NAME, locale);

                if (compressCommand.equals(text)) {
                    ConvertState convertState = commandStateService.getState(message.getChatId(), ConverterCommandNames.COMPRESS_AUDIO, false, ConvertState.class);

                    if (convertState == null || convertState.getFiles().isEmpty()) {
                        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESS_FILE_NOT_FOUND, locale))
                                .build());
                    } else {
                        workQueueJob.removeAndCancelCurrentTasks(message.getChatId());
                        convertionService.createConversion(message.getFrom(), convertState, Format.COMPRESS, locale);
                        commandStateService.deleteState(message.getChatId(), ConverterCommandNames.COMPRESS_AUDIO);
                    }
                } else {
                    stateFather.bitrate(message.getChatId(), text);
                }
            } else {
                ConvertState convertState = createState(message);
                messageService.sendMessage(
                        SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                .text(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESSION_SETTINGS, new Object[]{
                                        AudioCompressionMode.AUTO.name(), AudioCompressionMode.AUTO.name()
                                }, locale))
                                .replyMarkup(inlineKeyboardService.getAudioCompressionSettingsKeyboard(locale))
                                .build(),
                        sent -> {
                            convertState.setAudioCompressSettingsMessageId(sent.getMessageId());
                            commandStateService.setState(sent.getChatId(), getCommandIdentifier(), convertState);
                        }
                );
            }
        });
    }

    @Override
    public void processNonCommandCallback(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.AUDIO_COMPRESS_MODE.getKey())) {
            stateFather.mode(callbackQuery, AudioCompressionMode.valueOf(requestParams.getString(ConverterArg.AUDIO_COMPRESS_MODE.getKey())));
        } else if (requestParams.contains(ConverterArg.GO_BACK.getKey())) {
            stateFather.goBack(callbackQuery.getMessage().getChatId());
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, ConverterCommandNames.COMPRESS_AUDIO);
    }

    private ConvertState createState(Message message) {
        ConvertState convertState = new ConvertState();
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        convertState.setMedia(media);

        return convertState;
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat().getCategory() != FormatCategory.AUDIO) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_AUDIO_COMPRESS_FILE_NOT_FOUND, locale));
        }
    }
}

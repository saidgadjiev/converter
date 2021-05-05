package ru.gadjini.telegram.converter.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.conversion.impl.FFmpegAudioCompressConverter;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
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
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CompressAudioCommand implements BotCommand, NavigableBotCommand, CallbackBotCommand {

    private static final Map<Format, Map<String, List<String>>> BITRATES_MAP = Map.of(
            Format.OPUS, new HashMap<>() {{
                put(null, List.of("7", "13", "18", FFmpegAudioCompressConverter.AUTO_BITRATE, "64", "96", "128"));
            }},
            Format.MP3, Map.of(
                    "22050", List.of("7", "13", "18", FFmpegAudioCompressConverter.AUTO_BITRATE, "64", "96", "128"),
                    FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY, List.of(FFmpegAudioCompressConverter.AUTO_BITRATE, "64", "96", "128")
            )
    );

    private static final List<Format> COMPRESSION_FORMATS = List.of(Format.OPUS, Format.MP3);

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private CommandStateService commandStateService;

    private ConvertionService convertionService;

    private WorkQueueJob workQueueJob;

    private MessageMediaService messageMediaService;

    private ApplicationProperties applicationProperties;

    @Autowired
    public CompressAudioCommand(@TgMessageLimitsControl MessageService messageService, UserService userService,
                                LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                                @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                CommandStateService commandStateService, ConvertionService convertionService,
                                WorkQueueJob workQueueJob, MessageMediaService messageMediaService,
                                ApplicationProperties applicationProperties) {
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandStateService = commandStateService;
        this.convertionService = convertionService;
        this.workQueueJob = workQueueJob;
        this.messageMediaService = messageMediaService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.AUDIO_CONVERTER);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        messageService.sendMessage(
                SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESS_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.audioCompressionKeyboard(message.getChatId(), locale))
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
        ConvertState existsState = commandStateService.getState(message.getChatId(), ConverterCommandNames.COMPRESS_AUDIO, false, ConvertState.class);

        if (existsState == null) {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            ConvertState convertState = createState(message, locale);
            List<String> bitrates = getBitrates(convertState.getSettings().getFormat(), convertState.getSettings()
                    .getFrequencyOrDefault(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY));
            List<String> frequencies = getFrequencies(convertState.getSettings().getFormat());
            messageService.sendMessage(
                    SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                            .text(buildSettingsMessage(convertState))
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(inlineKeyboardService.getAudioCompressionSettingsKeyboard(
                                    convertState.getSettings().getBitrate(),
                                    convertState.getSettings().getFrequencyOrDefault(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY),
                                    convertState.getSettings().getFormat(),
                                    COMPRESSION_FORMATS,
                                    frequencies,
                                    bitrates, locale
                            )).build(),
                    sent -> {
                        convertState.getSettings().setMessageId(sent.getMessageId());
                        commandStateService.setState(sent.getChatId(), getCommandIdentifier(), convertState);
                    }
            );
        } else {
            updateState(existsState, message);
            updateSettingsMessage(message.getChatId(), existsState);
            commandStateService.setState(message.getChatId(), getCommandIdentifier(), existsState);
        }
    }

    @Override
    public void processNonCommandCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.COMPRESS.getKey())) {
            ConvertState convertState = commandStateService.getState(callbackQuery.getMessage().getChatId(),
                    ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);

            if (convertState != null) {
                workQueueJob.cancelCurrentTasks(callbackQuery.getMessage().getChatId());
                convertionService.createConversion(callbackQuery.getFrom(), convertState, Format.COMPRESS, new Locale(convertState.getUserLanguage()));
                commandStateService.deleteState(callbackQuery.getMessage().getChatId(), ConverterCommandNames.COMPRESS_AUDIO);
                messageService.removeInlineKeyboard(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
            }
        } else if (requestParams.contains(ConverterArg.BITRATE.getKey())) {
            String bitrate = requestParams.getString(ConverterArg.BITRATE.getKey());
            setBitrate(callbackQuery.getId(), callbackQuery.getMessage().getChatId(), bitrate);
        } else if (requestParams.contains(ConverterArg.COMPRESSION_FREQUENCY.getKey())) {
            String frequency = requestParams.getString(ConverterArg.COMPRESSION_FREQUENCY.getKey());
            setFrequency(callbackQuery.getId(), callbackQuery.getMessage().getChatId(), frequency);
        } else if (requestParams.contains(ConverterArg.COMPRESSION_FORMAT.getKey())) {
            Format compressionFormat = requestParams.get(ConverterArg.COMPRESSION_FORMAT.getKey(), Format::valueOf);
            if (COMPRESSION_FORMATS.contains(compressionFormat)) {
                setCompressionFormat(callbackQuery.getId(), callbackQuery.getMessage().getChatId(), compressionFormat);
            }
        }
    }

    @Override
    public void leave(long chatId) {
        ConvertState state = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, false, ConvertState.class);
        if (state != null) {
            commandStateService.deleteState(chatId, ConverterCommandNames.COMPRESS_AUDIO);
            messageService.removeInlineKeyboard(chatId, state.getSettings().getMessageId());
        }
    }

    private void updateSettingsMessage(long chatId, ConvertState convertState) {
        List<String> bitrates = getBitrates(convertState.getSettings().getFormat(), convertState.getSettings().getFrequencyOrDefault(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY));
        List<String> frequencies = getFrequencies(convertState.getSettings().getFormat());
        messageService.editMessage(EditMessageText.builder().chatId(String.valueOf(chatId))
                .messageId(convertState.getSettings().getMessageId())
                .text(buildSettingsMessage(convertState))
                .parseMode(ParseMode.HTML)
                .replyMarkup(inlineKeyboardService.getAudioCompressionSettingsKeyboard(convertState.getSettings().getBitrate(),
                        convertState.getSettings().getFrequencyOrDefault(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY),
                        convertState.getSettings().getFormat(), COMPRESSION_FORMATS,
                        frequencies, bitrates, new Locale(convertState.getUserLanguage())))
                .build());
    }

    private List<String> getBitrates(Format format, String frequency) {
        return BITRATES_MAP.get(format).get(frequency);
    }

    private List<String> getFrequencies(Format format) {
        return BITRATES_MAP.get(format).keySet().stream()
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());
        convertState.getSettings().setBitrate(FFmpegAudioCompressConverter.AUTO_BITRATE);
        convertState.getSettings().setFormat(Format.MP3);
        convertState.getSettings().setFrequency(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY);
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESS_FILE_NOT_FOUND, locale);
        convertState.setMedia(media);

        return convertState;
    }

    private void updateState(ConvertState convertState, Message message) {
        Locale locale = new Locale(convertState.getUserLanguage());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESSION_CHOOSE_BITRATE, locale);
        convertState.setMedia(media);
    }

    private void setBitrate(String queryId, long chatId, String bitrate) {
        ConvertState convertState = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);
        Locale locale = new Locale(convertState.getUserLanguage());

        String oldBitrate = convertState.getSettings().getBitrate();
        convertState.getSettings().setBitrate(bitrate);
        if (!Objects.equals(bitrate, oldBitrate)) {
            updateSettingsMessage(chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.COMPRESS_AUDIO, convertState);

        messageService.sendAnswerCallbackQuery(
                AnswerCallbackQuery.builder().callbackQueryId(queryId)
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESS_AUDIO_BITRATE_UPDATED, locale))
                        .build()
        );
    }

    private void setFrequency(String queryId, long chatId, String frequency) {
        ConvertState convertState = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);
        Locale locale = new Locale(convertState.getUserLanguage());

        String oldFrequency = convertState.getSettings().getFrequencyOrDefault(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY);
        convertState.getSettings().setFrequency(frequency);
        if (!Objects.equals(frequency, oldFrequency)) {
            updateSettingsMessage(chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.COMPRESS_AUDIO, convertState);

        messageService.sendAnswerCallbackQuery(
                AnswerCallbackQuery.builder().callbackQueryId(queryId)
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESS_AUDIO_FREQUENCY_UPDATED, locale))
                        .build()
        );
    }

    private void setCompressionFormat(String queryId, long chatId, Format format) {
        ConvertState convertState = commandStateService.getState(chatId, ConverterCommandNames.COMPRESS_AUDIO, true, ConvertState.class);
        Locale locale = new Locale(convertState.getUserLanguage());

        Format oldFormat = convertState.getSettings().getFormat();
        if (Format.OPUS.equals(format)) {
            convertState.getSettings().setFrequency(null);
        } else {
            convertState.getSettings().setFrequency(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY);
        }
        convertState.getSettings().setFormat(format);
        if (!Objects.equals(oldFormat, format)) {
            updateSettingsMessage(chatId, convertState);
        }
        commandStateService.setState(chatId, ConverterCommandNames.COMPRESS_AUDIO, convertState);

        messageService.sendAnswerCallbackQuery(
                AnswerCallbackQuery.builder().callbackQueryId(queryId)
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESS_AUDIO_FORMAT_UPDATED, locale))
                        .build()
        );
    }

    private void checkMedia(MessageMedia media, String errorMessageCode, Locale locale) {
        if (media == null || media.getFormat().getCategory() != FormatCategory.AUDIO) {
            throw new UserException(localisationService.getMessage(errorMessageCode, locale));
        }
    }

    private String buildSettingsMessage(ConvertState convertState) {
        Format format = convertState.getSettings().getFormatOrDefault(Format.MP3);
        StringBuilder message = new StringBuilder();
        Locale locale = new Locale(convertState.getUserLanguage());
        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESS_AUDIO_OUTPUT_FORMAT,
                new Object[]{format.getName()}, locale)).append("\n");
        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILE_FORMAT, new Object[]{convertState.getFirstFormat().getName()}, locale)).append("\n");

        if (format.equals(Format.MP3)) {
            message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESS_AUDIO_OUTPUT_FREQUENCY,
                    new Object[]{convertState.getSettings().getFrequencyOrDefault(FFmpegAudioCompressConverter.DEFAULT_MP3_FREQUENCY)}, locale)).append("\n");
        }
        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESSION_BITRATE, new Object[]{convertState.getSettings().getBitrate()}, locale)).append("\n");
        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESS_ORIGINAL_SIZE,
                new Object[]{MemoryUtils.humanReadableByteCount(convertState.getFirstFile().getFileSize())}, locale)).append("\n");

        String estimatedSize = estimatedSize(convertState.getFirstFile().getDuration(), convertState.getSettings().getBitrate());

        if (StringUtils.isNotBlank(estimatedSize)) {
            message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESS_OPUS_ESTIMATED_SIZE, new Object[]{estimatedSize}, locale)).append("\n\n");
        } else {
            message.append("\n");
        }

        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESS_PROMPT_BITRATE, locale)).append("\n\n");
        message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESSION_CHOOSE_BITRATE, locale));

        return message.toString();
    }

    private String estimatedSize(Integer duration, String bitrate) {
        if (duration == null) {
            return null;
        }

        return MemoryUtils.humanReadableByteCount((long) (duration * Double.parseDouble(bitrate) * 1000 / 8));
    }
}

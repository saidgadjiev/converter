package ru.gadjini.telegram.converter.command.bot;

import com.antkorwin.xsync.XSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
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
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class MergePdfsCommand implements BotCommand, NavigableBotCommand {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private MessageMediaService messageMediaService;

    private CommandStateService commandStateService;

    private WorkQueueJob workQueueJob;

    private ConvertionService convertionService;

    @Value("${converter:all}")
    private String converter;

    private XSync<Long> longXSync;

    @Autowired
    public MergePdfsCommand(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                            UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                            MessageMediaService messageMediaService, CommandStateService commandStateService,
                            ConvertionService convertionService, XSync<Long> longXSync) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.messageMediaService = messageMediaService;
        this.commandStateService = commandStateService;
        this.convertionService = convertionService;
        this.longXSync = longXSync;
    }

    @Autowired
    public void setWorkQueueJob(WorkQueueJob workQueueJob) {
        this.workQueueJob = workQueueJob;
    }

    @Override
    public boolean accept(Message message) {
        return Set.of(FormatsConfiguration.ALL_CONVERTER, FormatsConfiguration.DOCUMENT_CONVERTER).contains(converter);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_PDFS_WELCOME, locale))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(replyKeyboardService.mergePdfsKeyboard(message.getChatId(), locale))
                        .build()
        );
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.MERGE_PDFS;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return ConverterCommandNames.MERGE_PDFS;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        longXSync.execute(message.getChatId(), () -> {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

            if (message.hasText()) {
                String mergeCommandName = localisationService.getMessage(ConverterMessagesProperties.MERGE_COMMAND_NAME, locale);
                String cancelFilesCommandName = localisationService.getMessage(ConverterMessagesProperties.CANCEL_MERGE_PDFS_COMMAND_NAME, locale);
                ConvertState mergePdfsState = commandStateService.getState(message.getChatId(), getCommandIdentifier(), false, ConvertState.class);
                if (mergePdfsState == null || mergePdfsState.getFiles().isEmpty()) {
                    throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_PDFS_NO_FILES, locale));
                }
                if (Objects.equals(mergeCommandName, text)) {
                    if (mergePdfsState.getFiles().size() == 1) {
                        throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_PDFS_MIN_2_FILES, locale));
                    }
                    workQueueJob.cancelCurrentTasks(message.getChatId());
                    convertionService.createConversion(message.getFrom(), mergePdfsState, Format.MERGE_PDFS, locale);
                    commandStateService.deleteState(message.getChatId(), ConverterCommandNames.MERGE_PDFS);
                } else if (Objects.equals(cancelFilesCommandName, text)) {
                    commandStateService.deleteState(message.getChatId(), ConverterCommandNames.MERGE_PDFS);
                    messageService.sendMessage(
                            SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                                    .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_PDFS_CANCELED, new Object[]{mergePdfsState.getFiles().size()}, locale))
                                    .parseMode(ParseMode.HTML)
                                    .build()
                    );
                }
            } else {
                ConvertState mergePdfsState = commandStateService.getState(message.getChatId(), getCommandIdentifier(), false, ConvertState.class);
                if (mergePdfsState == null) {
                    mergePdfsState = createState(message);
                    commandStateService.setState(message.getChatId(), getCommandIdentifier(), mergePdfsState);
                } else {
                    MessageMedia media = messageMediaService.getMedia(message, locale);
                    if (media != null && media.getFormat() == Format.PDF) {
                        mergePdfsState.addMedia(media);
                        commandStateService.setState(message.getChatId(), getCommandIdentifier(), mergePdfsState);
                    }
                }
            }
        });
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getCommandIdentifier());
    }

    private ConvertState createState(Message message) {
        ConvertState mergePdfsState = new ConvertState();
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        mergePdfsState.setUserLanguage(locale.getLanguage());
        mergePdfsState.setMessageId(message.getMessageId());
        MessageMedia media = messageMediaService.getMedia(message, locale);

        checkMedia(media, locale);
        mergePdfsState.addMedia(media);

        return mergePdfsState;
    }

    private void checkMedia(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat() != Format.PDF) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_PDFS_NON_PDF_FILE, locale));
        }
    }
}

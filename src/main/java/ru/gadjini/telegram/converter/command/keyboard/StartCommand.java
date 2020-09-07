package ru.gadjini.telegram.converter.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class StartCommand implements NavigableBotCommand, BotCommand {

    private CommandStateService commandStateService;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ConverterReplyKeyboardService replyKeyboardService;

    private ConvertMaker convertMaker;

    @Autowired
    public StartCommand(CommandStateService commandStateService, UserService userService,
                        @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService,
                        @Qualifier("curr") ConverterReplyKeyboardService replyKeyboardService) {
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
    }

    @Autowired
    public void setConvertMaker(ConvertMaker convertMaker) {
        this.convertMaker = convertMaker;
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        convertMaker.processNonCommandUpdate(getHistoryName(), message, text, () -> getKeyboard(message.getChatId()));
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
        processMessage0(message.getChatId(), message.getFrom().getId());
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.START_COMMAND;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(
                new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                        .setReplyMarkup(replyKeyboardService.removeKeyboard(chatId))
        );
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
}

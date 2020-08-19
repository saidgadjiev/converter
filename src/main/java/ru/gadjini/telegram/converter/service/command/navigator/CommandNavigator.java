package ru.gadjini.telegram.converter.service.command.navigator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.bot.command.api.BotCommand;
import ru.gadjini.telegram.converter.bot.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.converter.bot.command.api.NavigableBotCommand;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.dao.command.navigator.keyboard.CommandNavigatorDao;
import ru.gadjini.telegram.converter.model.TgMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.converter.utils.ReflectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class CommandNavigator {

    private Map<String, NavigableBotCommand> navigableBotCommands = new HashMap<>();

    private CommandNavigatorDao navigatorDao;

    @Autowired
    public CommandNavigator(@Qualifier("redis") CommandNavigatorDao navigatorDao) {
        this.navigatorDao = navigatorDao;
    }

    @Autowired
    public void setBotCommands(Collection<BotCommand> botCommands) {
        ReflectionUtils.findImplements(botCommands, NavigableBotCommand.class).forEach(command -> navigableBotCommands.put(command.getHistoryName(), command));
    }

    @Autowired
    public void setKeyboardCommands(Collection<KeyboardBotCommand> keyboardCommands) {
        ReflectionUtils.findImplements(keyboardCommands, NavigableBotCommand.class).forEach(command -> navigableBotCommands.put(command.getHistoryName(), command));
    }

    public void push(long chatId, NavigableBotCommand navigableBotCommand) {
        NavigableBotCommand currCommand = getCurrentCommand(chatId);

        if (currCommand != null) {
            if (Objects.equals(currCommand.getHistoryName(), navigableBotCommand.getHistoryName())) {
                return;
            }
            if (!navigableBotCommand.setPrevCommand(chatId, currCommand.getHistoryName())) {
                currCommand.leave(chatId);
            }
        }

        setCurrentCommand(chatId, navigableBotCommand);
    }

    public boolean isEmpty(long chatId) {
        return navigatorDao.get(chatId) == null;
    }

    public void pop(TgMessage message) {
        long chatId = message.getChatId();
        NavigableBotCommand currentCommand = getCurrentCommand(chatId);
        if (currentCommand == null) {
            NavigableBotCommand parentCommand = navigableBotCommands.get(CommandNames.START_COMMAND);

            setCurrentCommand(chatId, parentCommand);
            parentCommand.restore(message);
        } else {
            if (currentCommand.canLeave(chatId)) {
                String parentHistoryName = currentCommand.getParentCommandName(chatId);

                if (StringUtils.isNotBlank(parentHistoryName)) {
                    currentCommand.leave(chatId);

                    NavigableBotCommand parentCommand = navigableBotCommands.get(parentHistoryName);
                    setCurrentCommand(chatId, parentCommand);
                    parentCommand.restore(message);
                } else {
                    currentCommand.restore(message);
                }
            } else {
                currentCommand.restore(message);
            }
        }
    }

    public SilentPop silentPop(long chatId) {
        NavigableBotCommand navigableBotCommand = getCurrentCommand(chatId);
        if (navigableBotCommand == null) {
            return null;
        }
        String parentHistoryName = navigableBotCommand.getParentCommandName(chatId);

        navigableBotCommand.leave(chatId);
        NavigableBotCommand parentCommand = navigableBotCommands.get(parentHistoryName);

        setCurrentCommand(chatId, parentCommand);

        return new SilentPop(parentCommand.getKeyboard(chatId), parentCommand.getMessage(chatId));
    }

    public void zeroRestore(long chatId, NavigableBotCommand botCommand) {
        setCurrentCommand(chatId, botCommand);
    }

    public NavigableBotCommand getCurrentCommand(long chatId) {
        String currCommand = navigatorDao.get(chatId);

        if (currCommand == null) {
            return null;
        }

        return navigableBotCommands.get(currCommand);
    }

    public void setCurrentCommand(long chatId, String command) {
        navigatorDao.set(chatId, command);
    }

    public boolean isCurrentCommandThat(long chatId, String expectedCommand) {
        String currCommand = navigatorDao.get(chatId);

        if (currCommand == null) {
            return false;
        }

        return currCommand.equals(expectedCommand);
    }

    private void setCurrentCommand(long chatId, NavigableBotCommand navigableBotCommand) {
        navigatorDao.set(chatId, navigableBotCommand.getHistoryName());
    }

    public static class SilentPop {

        private ReplyKeyboard replyKeyboardMarkup;

        private String message;

        public SilentPop(ReplyKeyboard replyKeyboardMarkup, String message) {
            this.replyKeyboardMarkup = replyKeyboardMarkup;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public ReplyKeyboard getReplyKeyboardMarkup() {
            return replyKeyboardMarkup;
        }
    }
}

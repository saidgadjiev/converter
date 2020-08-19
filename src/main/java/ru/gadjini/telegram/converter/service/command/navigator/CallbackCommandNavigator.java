package ru.gadjini.telegram.converter.service.command.navigator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.bot.command.api.BotCommand;
import ru.gadjini.telegram.converter.bot.command.api.CallbackBotCommand;
import ru.gadjini.telegram.converter.bot.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.converter.bot.command.api.NavigableCallbackBotCommand;
import ru.gadjini.telegram.converter.dao.command.navigator.callback.CallbackCommandNavigatorDao;
import ru.gadjini.telegram.converter.model.TgMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.telegram.converter.request.RequestParams;
import ru.gadjini.telegram.converter.utils.ReflectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class CallbackCommandNavigator {

    private Map<String, NavigableCallbackBotCommand> navigableBotCommands = new HashMap<>();

    private CallbackCommandNavigatorDao navigatorDao;

    @Autowired
    public CallbackCommandNavigator(@Qualifier("redis") CallbackCommandNavigatorDao navigatorDao) {
        this.navigatorDao = navigatorDao;
    }

    @Autowired
    public void setKeyboardCommands(Collection<KeyboardBotCommand> keyboardCommands) {
        ReflectionUtils.findImplements(keyboardCommands, NavigableCallbackBotCommand.class).forEach(command -> navigableBotCommands.put(command.getName(), command));
    }

    @Autowired
    public void setCallbackCommands(Collection<CallbackBotCommand> callbackCommands) {
        ReflectionUtils.findImplements(callbackCommands, NavigableCallbackBotCommand.class).forEach(command -> navigableBotCommands.put(command.getName(), command));
    }

    @Autowired
    public void setBotCommands(Collection<BotCommand> botCommands) {
        ReflectionUtils.findImplements(botCommands, NavigableCallbackBotCommand.class).forEach(command -> navigableBotCommands.put(command.getName(), command));
    }

    public void push(long chatId, NavigableCallbackBotCommand callbackBotCommand) {
        if (callbackBotCommand.isAcquireKeyboard()) {
            navigatorDao.set(chatId, callbackBotCommand.getName());
        }
    }

    public void popTo(TgMessage message, String commandName, RequestParams requestParams) {
        NavigableCallbackBotCommand currCommand = getCurrentCommand(message.getChatId());
        if (currCommand != null) {
            currCommand.leave(message.getChatId());
        }
        navigatorDao.delete(message.getChatId());

        NavigableCallbackBotCommand callbackBotCommand = navigableBotCommands.get(commandName);
        callbackBotCommand.restore(message, null, requestParams);
    }

    public ReplyKeyboardMarkup silentPop(long chatId) {
        NavigableCallbackBotCommand currCommand = getCurrentCommand(chatId);
        if (currCommand != null) {
            currCommand.leave(chatId);
        }
        navigatorDao.delete(chatId);

        return null;
    }

    public NavigableCallbackBotCommand getCurrentCommand(long chatId) {
        String currentCommand = navigatorDao.get(chatId);

        if (StringUtils.isBlank(currentCommand)) {
            return null;
        }

        return navigableBotCommands.get(currentCommand);
    }
}

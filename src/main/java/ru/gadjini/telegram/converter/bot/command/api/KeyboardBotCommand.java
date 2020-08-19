package ru.gadjini.telegram.converter.bot.command.api;

import ru.gadjini.telegram.converter.model.bot.api.object.Message;

public interface KeyboardBotCommand extends MyBotCommand {

    boolean canHandle(long chatId, String command);
    
    default boolean isTextCommand() {
        return false;
    }

    boolean processMessage(Message message, String text);
}

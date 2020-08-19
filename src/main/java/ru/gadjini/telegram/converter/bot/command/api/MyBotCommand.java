package ru.gadjini.telegram.converter.bot.command.api;

import ru.gadjini.telegram.converter.model.bot.api.object.Message;

public interface MyBotCommand {

    default void processNonCommandUpdate(Message message, String text) {
    }

    default boolean accept(Message message) {
        return message.hasText();
    }

    default void cancel(long chatId, String queryId) {}
}

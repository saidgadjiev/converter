package ru.gadjini.telegram.converter.bot.command.api;

import ru.gadjini.telegram.converter.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.converter.request.RequestParams;

public interface CallbackBotCommand extends MyBotCommand {

    String getName();

    /**
     */
    void processMessage(CallbackQuery callbackQuery, RequestParams requestParams);

    default void processNonCommandCallback(CallbackQuery callbackQuery, RequestParams requestParams) {

    }
}

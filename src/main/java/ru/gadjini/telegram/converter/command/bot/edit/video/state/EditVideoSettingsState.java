package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.command.bot.edit.video.EditVideoCommand;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

public interface EditVideoSettingsState {

    default void enter(long userId, EditVideoState currentState) {
        throw new UnsupportedOperationException();
    }

    default boolean enter(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery, EditVideoState currentState) {
        return true;
    }

    default void goBack(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery, EditVideoState currentState) {

    }

    default void callbackUpdate(EditVideoCommand editVideoCommand, CallbackQuery callbackQuery,
                                RequestParams requestParams, EditVideoState currentState) {

    }

    EditVideoSettingsStateName getName();
}

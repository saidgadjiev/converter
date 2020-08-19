package ru.gadjini.telegram.converter.dao.command.navigator.callback;

public interface CallbackCommandNavigatorDao {
    void set(long chatId, String command);

    String get(long chatId);

    void delete(long chatId);
}

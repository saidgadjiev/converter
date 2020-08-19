package ru.gadjini.telegram.converter.dao.command.navigator.keyboard;

public interface CommandNavigatorDao {
    void set(long chatId, String command);

    String get(long chatId);

}

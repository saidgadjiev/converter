package ru.gadjini.telegram.converter.dao.command.state;

import java.util.concurrent.TimeUnit;

public interface CommandStateDao {
    void setState(long chatId, String command, Object state, long ttl, TimeUnit timeUnit);

    <T> T getState(long chatId, String command, Class<T> tClass);

    boolean hasState(long chatId, String command);

    void deleteState(long chatId, String command);
}

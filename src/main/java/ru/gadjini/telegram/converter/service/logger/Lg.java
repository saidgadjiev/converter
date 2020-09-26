package ru.gadjini.telegram.converter.service.logger;

public interface Lg extends AutoCloseable {

    void log(String log, Object... args);

    @Override
    void close();
}

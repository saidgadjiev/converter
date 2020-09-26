package ru.gadjini.telegram.converter.service.logger;

public class SoutLg implements Lg {
    @Override
    public void log(String log, Object... args) {
        System.out.println(String.format(log, args));
    }

    @Override
    public void close() {

    }
}

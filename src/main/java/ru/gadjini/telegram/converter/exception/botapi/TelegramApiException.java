package ru.gadjini.telegram.converter.exception.botapi;

public class TelegramApiException extends RuntimeException {
    public TelegramApiException() {
        super();
    }

    public TelegramApiException(String message) {
        super(message);
    }

    public TelegramApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public TelegramApiException(Throwable cause) {
        super(cause);
    }

    protected TelegramApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

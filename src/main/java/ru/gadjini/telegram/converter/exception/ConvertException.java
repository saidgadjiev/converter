package ru.gadjini.telegram.converter.exception;

public class ConvertException extends RuntimeException {

    public ConvertException(Throwable ex) {
        super(ex);
    }

    public ConvertException(String message) {
        super(message);
    }

    public ConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}

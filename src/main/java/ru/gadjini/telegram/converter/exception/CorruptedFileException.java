package ru.gadjini.telegram.converter.exception;

public class CorruptedFileException extends RuntimeException {

    public CorruptedFileException(String message) {
        super(message);
    }
}

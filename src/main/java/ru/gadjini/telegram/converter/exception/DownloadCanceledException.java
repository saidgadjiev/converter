package ru.gadjini.telegram.converter.exception;

public class DownloadCanceledException extends RuntimeException {

    public DownloadCanceledException(String message) {
        super(message);
    }
}

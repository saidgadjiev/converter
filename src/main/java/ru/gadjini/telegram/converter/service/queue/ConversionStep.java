package ru.gadjini.telegram.converter.service.queue;

public enum ConversionStep {

    WAITING,

    DOWNLOADING,

    CONVERTING,

    UPLOADING,

    COMPLETED
}

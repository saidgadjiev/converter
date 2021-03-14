package ru.gadjini.telegram.converter.request;

public enum ConverterArg {

    COMPRESS("f"),
    RESOLUTION("i"),
    EDIT_VIDEO("l");

    private final String key;

    ConverterArg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

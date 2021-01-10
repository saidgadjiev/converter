package ru.gadjini.telegram.converter.request;

public enum ConverterArg {

    AUTO_BIT_RATE("e");

    private final String key;

    ConverterArg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
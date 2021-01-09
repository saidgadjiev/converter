package ru.gadjini.telegram.converter.request;

public enum ConverterArg {

    AUDIO_COMPRESS_MODE("e"),
    GO_BACK("f");

    private final String key;

    ConverterArg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

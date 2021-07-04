package ru.gadjini.telegram.converter.request;

public enum ConverterArg {

    COMPRESS("f"),
    BITRATE("g"),
    RESOLUTION("i"),
    EDIT_VIDEO("l"),
    COMPRESSION_FORMAT("k"),
    COMPRESSION_FREQUENCY("v"),
    LANGUAGE("kv"),
    GO_BACK("gb");

    private final String key;

    ConverterArg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

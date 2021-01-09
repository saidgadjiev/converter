package ru.gadjini.telegram.converter.request;

public enum Arg {

    QUEUE_ITEM_ID("a"),
    PREV_HISTORY_NAME("b"),
    CALLBACK_DELEGATE("d"),
    AUDIO_COMPRESS_MODE("e");

    private final String key;

    Arg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

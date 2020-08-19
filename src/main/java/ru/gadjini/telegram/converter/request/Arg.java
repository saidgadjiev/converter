package ru.gadjini.telegram.converter.request;

public enum Arg {

    QUEUE_ITEM_ID("a"),
    PREV_HISTORY_NAME("b"),
    ACTION_FROM("c"),
    CALLBACK_DELEGATE("d"),
    TRANSPARENT_MODE("e"),
    TRANSPARENT_COLOR("f"),
    GO_BACK("g"),
    EDIT_STATE_NAME("k"),
    IMAGE_FILTER("m"),
    INACCURACY("l"),
    IMAGE_SIZE("o"),
    UPDATE_EDITED_IMAGE("p"),
    EXTRACT_FILE_ID("r"),
    JOB_ID("s"),
    OFFSET("t"),
    PAGINATION("u"),
    PREV_LIMIT("v");

    private final String key;

    Arg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

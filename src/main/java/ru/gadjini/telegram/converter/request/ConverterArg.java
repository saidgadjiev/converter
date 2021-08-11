package ru.gadjini.telegram.converter.request;

public enum ConverterArg {

    COMPRESS("f"),
    BITRATE("g"),
    RESOLUTION("i"),
    EDIT_VIDEO("l"),
    COMPRESSION_FORMAT("k"),
    COMPRESSION_FREQUENCY("v"),
    LANGUAGE("kv"),
    GO_BACK("gb"),
    VAV_MERGE_AUDIO_MODE("va"),
    VAV_MERGE_SUBTITLES_MODE("vs"),
    VAV_MERGE("vm"),
    VEDIT_CHOOSE_RESOLUTION("vt"),
    VEDIT_CHOOSE_CRF("vc"),
    VEDIT_CHOOSE_AUDIO_CODEC("vac"),
    CRF("cf"),
    AUDIO_CODEC("acdc");

    private final String key;

    ConverterArg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

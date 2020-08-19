package ru.gadjini.telegram.converter.model.bot.api;

public enum MediaType {

    PHOTO_THUMBNAIL(0),
    CHAT_PHOTO(1),
    PHOTO(2),
    VOICE(3),
    VIDEO(4),
    DOCUMENT(5),
    STICKER(8),
    AUDIO(9),
    ANIMATION(10),
    VIDEO_NOTE(13),
    DOCUMENT_THUMBNAIL(14);

    private final int code;

    MediaType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MediaType fromCode(int code) {
        for (MediaType mediaType : values()) {
            if (mediaType.code == code) {
                return mediaType;
            }
        }

        return MediaType.DOCUMENT;
    }
}

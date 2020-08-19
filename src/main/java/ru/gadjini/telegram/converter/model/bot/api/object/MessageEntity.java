package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageEntity {
    private static final String TYPE_FIELD = "type";
    private static final String OFFSET_FIELD = "offset";
    /**
     * Type of the entity. One of
     * mention (@username),
     * hashtag,
     * cashtag
     * bot_command,
     * url,
     * email,
     * phone_number,
     * bold (bold text),
     * italic (italic text),
     * code (monowidth string),
     * pre (monowidth block),
     * text_link (for clickable text URLs),
     * text_mention (for users without usernames),
     * underline,
     * strikethrough
     */

    @JsonProperty(TYPE_FIELD)
    private String type;
    @JsonProperty(OFFSET_FIELD)
    private Integer offset; ///< Offset in UTF-16 code units to the start of the entity

    public MessageEntity() {
        super();
    }

    public String getType() {
        return type;
    }

    public Integer getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "type='" + type + '\'' +
                ", offset=" + offset +
                '}';
    }
}

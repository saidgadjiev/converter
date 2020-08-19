package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

public class Message {
    private static final String MESSAGEID_FIELD = "message_id";
    private static final String FROM_FIELD = "from_user";
    private static final String FROM = "from";
    private static final String CHAT_FIELD = "chat";
    private static final String TEXT_FIELD = "text";
    private static final String ENTITIES_FIELD = "entities";
    private static final String AUDIO_FIELD = "audio";
    private static final String DOCUMENT_FIELD = "document";
    private static final String PHOTO_FIELD = "photo";
    private static final String STICKER_FIELD = "sticker";
    private static final String VIDEO_FIELD = "video";
    private static final String REPLY_MARKUP_FIELD = "reply_markup";

    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId;
    @JsonProperty(FROM_FIELD)
    @JsonAlias(FROM)
    private User from;
    @JsonProperty(CHAT_FIELD)
    private Chat chat;
    @JsonProperty(TEXT_FIELD)
    private String text;
    @JsonProperty(ENTITIES_FIELD)
    private List<MessageEntity> entities;
    @JsonProperty(REPLY_MARKUP_FIELD)
    private InlineKeyboardMarkup replyMarkup;
    @JsonProperty(AUDIO_FIELD)
    private Audio audio;
    @JsonProperty(DOCUMENT_FIELD)
    private Document document;
    @JsonProperty(PHOTO_FIELD)
    private List<PhotoSize> photo;
    @JsonProperty(STICKER_FIELD)
    private Sticker sticker;
    @JsonProperty(VIDEO_FIELD)
    private Video video;

    public Message() {
        super();
    }

    public Integer getMessageId() {
        return messageId;
    }

    public User getFrom() {
        return from;
    }

    public Chat getChat() {
        return chat;
    }

    public String getText() {
        return text;
    }

    public Audio getAudio() {
        return audio;
    }

    public Document getDocument() {
        return document;
    }

    public List<PhotoSize> getPhoto() {
        return photo;
    }

    public Sticker getSticker() {
        return sticker;
    }

    public boolean hasSticker() {
        return sticker != null;
    }

    public Video getVideo() {
        return video;
    }

    public Long getChatId() {
        return chat.getId();
    }

    public boolean hasText() {
        return text != null && !text.isEmpty();
    }

    public boolean isCommand() {
        if (hasText() && entities != null) {
            for (MessageEntity entity : entities) {
                if (entity != null && entity.getOffset() == 0 &&
                        EntityType.BOTCOMMAND.equals(entity.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasDocument() {
        return this.document != null;
    }

    public boolean hasVideo() {
        return this.video != null;
    }

    public boolean hasAudio(){
        return this.audio != null;
    }

    public boolean hasEntities() {
        return entities != null && !entities.isEmpty();
    }

    public boolean hasPhoto() {
        return photo != null && !photo.isEmpty();
    }

    public boolean hasReplyMarkup() {
        return replyMarkup != null;
    }

    public InlineKeyboardMarkup getReplyMarkup() {
        return replyMarkup;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", from=" + from +
                ", chat=" + chat +
                ", text='" + text + '\'' +
                ", entities=" + entities +
                ", audio=" + audio +
                ", document=" + document +
                ", photo=" + photo +
                ", sticker=" + sticker +
                ", video=" + video +
                '}';
    }
}

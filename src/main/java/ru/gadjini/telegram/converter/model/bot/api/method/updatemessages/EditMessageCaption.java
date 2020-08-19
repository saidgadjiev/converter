package ru.gadjini.telegram.converter.model.bot.api.method.updatemessages;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

public class EditMessageCaption {
    public static final String METHOD = "editmessagecaption";

    private static final String CHATID_FIELD = "chat_id";
    private static final String MESSAGEID_FIELD = "message_id";
    private static final String CAPTION_FIELD = "caption";
    private static final String REPLYMARKUP_FIELD = "reply_markup";
    private static final String PARSEMODE_FIELD = "parse_mode";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId;
    @JsonProperty(CAPTION_FIELD)
    private String caption;
    @JsonProperty(REPLYMARKUP_FIELD)
    private InlineKeyboardMarkup replyMarkup;
    @JsonProperty(PARSEMODE_FIELD)
    private String parseMode;

    public EditMessageCaption() {
        super();
    }

    public EditMessageCaption(Long chatId, int messageId, String caption) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.caption = caption;
    }

    public String getChatId() {
        return chatId;
    }

    public EditMessageCaption setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public EditMessageCaption setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public EditMessageCaption setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public InlineKeyboardMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public EditMessageCaption setReplyMarkup(InlineKeyboardMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    public String getParseMode() {
        return parseMode;
    }

    public EditMessageCaption setParseMode(String parseMode) {
        this.parseMode = parseMode;
        return this;
    }

    @Override
    public String toString() {
        return "EditMessageCaption{" +
                "chatId='" + chatId + '\'' +
                ", messageId=" + messageId +
                ", caption='" + caption + '\'' +
                ", replyMarkup=" + replyMarkup +
                ", parseMode='" + parseMode + '\'' +
                '}';
    }
}

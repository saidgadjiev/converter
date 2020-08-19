package ru.gadjini.telegram.converter.model.bot.api.method.updatemessages;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

public class EditMessageReplyMarkup {
    public static final String METHOD = "editmessagereplymarkup";

    private static final String CHATID_FIELD = "chat_id";
    private static final String MESSAGEID_FIELD = "message_id";
    private static final String REPLYMARKUP_FIELD = "reply_markup";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId;
    @JsonProperty(REPLYMARKUP_FIELD)
    private InlineKeyboardMarkup replyMarkup;

    public EditMessageReplyMarkup() {
    }

    public EditMessageReplyMarkup(Long chatId, Integer messageId) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public EditMessageReplyMarkup setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public EditMessageReplyMarkup setChatId(Long chatId) {
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public EditMessageReplyMarkup setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    public InlineKeyboardMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public EditMessageReplyMarkup setReplyMarkup(InlineKeyboardMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    @Override
    public String toString() {
        return "EditMessageReplyMarkup{" +
                "chatId=" + chatId +
                ", messageId=" + messageId +
                ", replyMarkup=" + replyMarkup +
                '}';
    }
}

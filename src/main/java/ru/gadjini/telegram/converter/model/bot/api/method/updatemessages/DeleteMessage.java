package ru.gadjini.telegram.converter.model.bot.api.method.updatemessages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeleteMessage {
    public static final String METHOD = "deletemessage";

    private static final String CHATID_FIELD = "chat_id";
    private static final String MESSAGEID_FIELD = "message_id";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId;

    public DeleteMessage() {
        super();
    }

    public DeleteMessage(String chatId, Integer messageId) {
        this.chatId = checkNotNull(chatId);
        this.messageId = checkNotNull(messageId);
    }

    public DeleteMessage(Long chatId, Integer messageId) {
        this.chatId = checkNotNull(chatId).toString();
        this.messageId = checkNotNull(messageId);
    }

    public String getChatId() {
        return chatId;
    }

    public DeleteMessage setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public DeleteMessage setChatId(Long chatId) {
        Objects.requireNonNull(chatId);
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public DeleteMessage setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    @Override
    public String toString() {
        return "DeleteMessage{" +
                "chatId='" + chatId + '\'' +
                ", messageId=" + messageId +
                '}';
    }
}

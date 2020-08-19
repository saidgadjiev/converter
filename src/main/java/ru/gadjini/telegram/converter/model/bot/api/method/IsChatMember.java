package ru.gadjini.telegram.converter.model.bot.api.method;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class IsChatMember {
    public static final String METHOD = "ischatmember";

    private static final String CHATID_FIELD = "chat_id";
    private static final String USERID_FIELD = "user_id";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(USERID_FIELD)
    private Integer userId;

    public String getChatId() {
        return chatId;
    }

    public IsChatMember setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public IsChatMember setChatId(Long chatId) {
        Objects.requireNonNull(chatId);
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getUserId() {
        return userId;
    }

    public IsChatMember setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public String toString() {
        return "IsChatMember{" +
                "chatId='" + chatId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}

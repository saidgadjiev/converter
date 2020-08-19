package ru.gadjini.telegram.converter.model;

import ru.gadjini.telegram.converter.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;
import ru.gadjini.telegram.converter.model.bot.api.object.User;

import java.util.ArrayList;
import java.util.List;

public class TgMessage {

    private long chatId;

    private int messageId;

    private String callbackQueryId;

    private User user;

    private String text;
    
    private List<MetaType> metaTypes;

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getCallbackQueryId() {
        return callbackQueryId;
    }

    public void setCallbackQueryId(String callbackQueryId) {
        this.callbackQueryId = callbackQueryId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<MetaType> getMetaTypes() {
        return metaTypes;
    }

    public void setMetaTypes(List<MetaType> metaTypes) {
        this.metaTypes = metaTypes;
    }

    public static TgMessage from(CallbackQuery callbackQuery) {
        TgMessage tgMessage = new TgMessage();

        tgMessage.chatId = callbackQuery.getMessage().getChatId();
        tgMessage.messageId = callbackQuery.getMessage().getMessageId();
        tgMessage.callbackQueryId = callbackQuery.getId();
        tgMessage.user = callbackQuery.getFrom();
        tgMessage.text = callbackQuery.getData();

        return tgMessage;
    }

    public static TgMessage from(Message message) {
        TgMessage tgMessage = new TgMessage();

        tgMessage.chatId = message.getChatId();
        tgMessage.messageId = message.getMessageId();
        tgMessage.user = message.getFrom();
        tgMessage.text = message.hasText() ? message.getText().trim() : "";
        tgMessage.setMetaTypes(getMetaTypes(message));

        return tgMessage;
    }

    public static TgMessage from(Update update) {
        if (update.hasCallbackQuery()) {
            return from(update.getCallbackQuery());
        }

        return from(update.getMessage());
    }

    public static long getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }

        return update.getMessage().getChatId();
    }

    public static int getUserId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        }

        return update.getMessage().getFrom().getId();
    }

    public static User getUser(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom();
        }

        return update.getMessage().getFrom();
    }

    public static List<MetaType> getMetaTypes(Message message) {
        List<MetaType> metaTypes = new ArrayList<>();

        if (message.hasDocument()) {
            metaTypes.add(MetaType.DOCUMENT);
        }
        if (message.hasText()) {
            metaTypes.add(MetaType.TEXT);
        }
        if (message.hasPhoto()) {
            metaTypes.add(MetaType.PHOTO);
        }
        if (message.hasVideo()) {
            metaTypes.add(MetaType.VIDEO);
        }
        if (message.hasAudio()) {
            metaTypes.add(MetaType.AUDIO);
        }

        return metaTypes;
    }

    @Override
    public String toString() {
        return "TgMessage{" +
                "chatId=" + chatId +
                ", messageId=" + messageId +
                ", callbackQueryId='" + callbackQueryId + '\'' +
                ", user=" + user +
                ", text='" + text + '\'' +
                ", metaTypes=" + metaTypes +
                '}';
    }

    public enum MetaType {

        TEXT,

        DOCUMENT,

        AUDIO,

        VIDEO,

        PHOTO,

        VOICE,

        CONTACT,

        LOCATION
    }
}

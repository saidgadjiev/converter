package ru.gadjini.telegram.converter.model.bot.api.method.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.InputFile;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.io.File;
import java.util.Objects;

public class SendSticker {
    public static final String METHOD = "sendsticker";

    public static final String CHATID_FIELD = "chat_id";
    public static final String STICKER_FIELD = "sticker";
    public static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    public static final String REPLYMARKUP_FIELD = "reply_markup";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(STICKER_FIELD)
    private InputFile sticker;
    @JsonProperty(REPLYTOMESSAGEID_FIELD)
    private Integer replyToMessageId;
    @JsonProperty(REPLYMARKUP_FIELD)
    private ReplyKeyboard replyMarkup;

    public SendSticker() {
        super();
    }

    public SendSticker(Long chatId, File file) {
        this.chatId = chatId.toString();
        this.sticker = new InputFile();
        this.sticker.setFilePath(file.getAbsolutePath());
    }

    public SendSticker(Long chatId, String fileId) {
        this.chatId = chatId.toString();
        this.sticker = new InputFile();
        this.sticker.setFileId(fileId);
    }

    public String getChatId() {
        return chatId;
    }

    public SendSticker setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public SendSticker setChatId(Long chatId) {
        Objects.requireNonNull(chatId);
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public SendSticker setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
        return this;
    }

    public ReplyKeyboard getReplyMarkup() {
        return replyMarkup;
    }

    public SendSticker setReplyMarkup(ReplyKeyboard replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    @Override
    public String toString() {
        return "SendSticker{" +
                "chatId='" + chatId + '\'' +
                ", sticker=" + sticker +
                ", replyToMessageId=" + replyToMessageId +
                ", replyMarkup=" + replyMarkup +
                '}';
    }
}

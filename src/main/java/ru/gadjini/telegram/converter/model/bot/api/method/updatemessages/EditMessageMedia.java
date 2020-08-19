package ru.gadjini.telegram.converter.model.bot.api.method.updatemessages;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.InputMedia;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

import java.io.File;

public class EditMessageMedia {
    public static final String METHOD = "editmessagemedia";

    public static final String CHATID_FIELD = "chat_id";
    public static final String MESSAGEID_FIELD = "message_id";
    public static final String REPLYMARKUP_FIELD = "reply_markup";
    public static final String MEDIA_FIELD = "media";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId;
    @JsonProperty(MEDIA_FIELD)
    private InputMedia media;
    @JsonProperty(REPLYMARKUP_FIELD)
    private InlineKeyboardMarkup replyMarkup;

    public EditMessageMedia() {
        super();
    }

    public EditMessageMedia(Long chatId, int messageId, File file, String caption) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.media = new InputMedia();
        this.media.setFilePath(file.getAbsolutePath());
        this.media.setCaption(caption);
    }

    public EditMessageMedia(Long chatId, int messageId, String fileName, File file, String caption) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.media = new InputMedia();
        this.media.setFilePath(file.getAbsolutePath());
        this.media.setCaption(caption);
        this.media.setFileName(fileName);
    }

    public EditMessageMedia(Long chatId, int messageId, File file) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.media = new InputMedia();
        this.media.setFilePath(file.getAbsolutePath());
        this.media.setFileName(file.getName());
    }

    public EditMessageMedia(Long chatId, int messageId, String fileName, File file) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.media = new InputMedia();
        this.media.setFilePath(file.getAbsolutePath());
        this.media.setFileName(fileName);
    }

    public EditMessageMedia(Long chatId, int messageId, String fileId) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.media = new InputMedia();
        this.media.setFileId(fileId);
    }


    public EditMessageMedia(Long chatId, int messageId, String fileId, String caption) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.media = new InputMedia();
        this.media.setFileId(fileId);
        this.media.setCaption(caption);
    }

    public String getChatId() {
        return chatId;
    }

    public EditMessageMedia setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public EditMessageMedia setChatId(Long chatId) {
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public EditMessageMedia setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    public InlineKeyboardMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public EditMessageMedia setReplyMarkup(InlineKeyboardMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    public InputMedia getMedia() {
        return media;
    }

    @Override
    public String toString() {
        return "EditMessageMedia{" +
                "chatId='" + chatId + '\'' +
                ", messageId=" + messageId +
                ", replyMarkup=" + replyMarkup +
                '}';
    }
}

package ru.gadjini.telegram.converter.model.bot.api.method.send;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.InputFile;
import ru.gadjini.telegram.converter.model.bot.api.object.Progress;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.io.File;
import java.util.Objects;
public class SendDocument {
    public static final String METHOD = "senddocument";

    public static final String CHATID_FIELD = "chat_id";
    public static final String DOCUMENT_FIELD = "document";
    public static final String CAPTION_FIELD = "caption";
    public static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    public static final String REPLYMARKUP_FIELD = "reply_markup";
    public static final String PARSEMODE_FIELD = "parse_mode";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonIgnore
    private long origChatId;
    @JsonProperty(DOCUMENT_FIELD)
    private InputFile document;
    @JsonProperty(CAPTION_FIELD)
    private String caption;
    @JsonProperty(REPLYTOMESSAGEID_FIELD)
    private Integer replyToMessageId;
    @JsonProperty(REPLYMARKUP_FIELD)
    private ReplyKeyboard replyMarkup;
    @JsonProperty(PARSEMODE_FIELD)
    private String parseMode;

    private Progress progress;

    public SendDocument() {
        super();
    }

    public SendDocument(Long chatId, File file) {
        this.chatId = chatId.toString();
        this.origChatId = chatId;
        this.document = new InputFile();
        this.document.setFilePath(file.getAbsolutePath());
        this.document.setFileName(file.getName());
    }

    public SendDocument(Long chatId, String fileName, File file) {
        this.chatId = chatId.toString();
        this.origChatId = chatId;
        this.document = new InputFile();
        this.document.setFilePath(file.getAbsolutePath());
        this.document.setFileName(fileName);
    }

    public SendDocument(Long chatId, String fileId) {
        this.chatId = chatId.toString();
        this.origChatId = chatId;
        this.document = new InputFile();
        this.document.setFileId(fileId);
    }

    public SendDocument setThumb(String thumb) {
        document.setThumb(thumb);

        return this;
    }

    public String getChatId() {
        return chatId;
    }

    public SendDocument setChatId(String chatId) {
        this.chatId = chatId;
        this.origChatId = -1;
        return this;
    }

    public SendDocument setChatId(Long chatId) {
        Objects.requireNonNull(chatId);
        this.chatId = chatId.toString();
        this.origChatId = chatId;
        return this;
    }

    public InputFile getDocument() {
        return document;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public SendDocument setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public SendDocument setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public ReplyKeyboard getReplyMarkup() {
        return replyMarkup;
    }

    public SendDocument setReplyMarkup(ReplyKeyboard replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    public String getParseMode() {
        return parseMode;
    }

    public SendDocument setParseMode(String parseMode) {
        this.parseMode = parseMode;
        return this;
    }

    public long getOrigChatId() {
        return origChatId;
    }

    public Progress getProgress() {
        return progress;
    }

    public SendDocument setProgress(Progress progress) {
        this.progress = progress;

        return this;
    }

    @Override
    public String toString() {
        return "SendDocument{" +
                "chatId='" + chatId + '\'' +
                ", document=" + document +
                ", setCaption='" + caption + '\'' +
                ", replyToMessageId=" + replyToMessageId +
                ", replyMarkup=" + replyMarkup +
                ", parseMode='" + parseMode + '\'' +
                '}';
    }
}

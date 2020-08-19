package ru.gadjini.telegram.converter.model.bot.api.method.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.InputFile;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.io.File;
import java.util.Objects;

public class SendAudio {
    public static final String METHOD = "sendaudio";

    public static final String CHATID_FIELD = "chat_id";
    private static final String VIDEO_FIELD = "audio";
    private static final String CAPTION_FIELD = "caption";
    private static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private static final String REPLYMARKUP_FIELD = "reply_markup";
    private static final String PARSEMODE_FIELD = "parse_mode";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(VIDEO_FIELD)
    private InputFile audio;
    @JsonProperty(CAPTION_FIELD)
    private String caption;
    @JsonProperty(REPLYTOMESSAGEID_FIELD)
    private Integer replyToMessageId;
    @JsonProperty(REPLYMARKUP_FIELD)
    private ReplyKeyboard replyMarkup;
    @JsonProperty(PARSEMODE_FIELD)
    private String parseMode;

    public SendAudio(Long chatId, File file) {
        this.chatId = chatId.toString();
        this.audio = new InputFile();
        this.audio.setFilePath(file.getAbsolutePath());
        this.audio.setFileName(file.getName());
    }

    public SendAudio(Long chatId, String fileName, File file) {
        this.chatId = chatId.toString();
        this.audio = new InputFile();
        this.audio.setFilePath(file.getAbsolutePath());
        this.audio.setFileName(fileName);
    }

    public SendAudio(Long chatId, String fileId) {
        this.chatId = chatId.toString();
        this.audio = new InputFile();
        this.audio.setFileId(fileId);
    }

    public String getChatId() {
        return chatId;
    }

    public SendAudio setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public InputFile getAudio() {
        return audio;
    }

    public SendAudio setAudio(String audio) {
        this.audio = new InputFile(audio);
        return this;
    }

    public SendAudio setChatId(Long chatId) {
        Objects.requireNonNull(chatId);
        this.chatId = chatId.toString();
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public SendAudio setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public SendAudio setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
        return this;
    }

    public ReplyKeyboard getReplyMarkup() {
        return replyMarkup;
    }

    public SendAudio setReplyMarkup(ReplyKeyboard replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    public String getParseMode() {
        return parseMode;
    }

    public SendAudio setParseMode(String parseMode) {
        this.parseMode = parseMode;
        return this;
    }

    @Override
    public String toString() {
        return "SendVideo{" +
                "chatId='" + chatId + '\'' +
                ", audio=" + audio +
                ", caption='" + caption + '\'' +
                ", replyToMessageId=" + replyToMessageId +
                ", replyMarkup=" + replyMarkup +
                ", parseMode='" + parseMode + '\'' +
                '}';
    }
}

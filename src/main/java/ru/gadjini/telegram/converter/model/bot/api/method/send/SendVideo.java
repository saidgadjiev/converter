package ru.gadjini.telegram.converter.model.bot.api.method.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.InputFile;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.io.File;
import java.util.Objects;

public class SendVideo {
    public static final String METHOD = "sendvideo";

    public static final String CHATID_FIELD = "chat_id";
    private static final String VIDEO_FIELD = "video";
    private static final String CAPTION_FIELD = "caption";
    private static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private static final String REPLYMARKUP_FIELD = "reply_markup";
    private static final String PARSEMODE_FIELD = "parse_mode";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(VIDEO_FIELD)
    private InputFile video;
    @JsonProperty(CAPTION_FIELD)
    private String caption;
    @JsonProperty(REPLYTOMESSAGEID_FIELD)
    private Integer replyToMessageId;
    @JsonProperty(REPLYMARKUP_FIELD)
    private ReplyKeyboard replyMarkup;
    @JsonProperty(PARSEMODE_FIELD)
    private String parseMode;

    public SendVideo(Long chatId, File file) {
        this.chatId = chatId.toString();
        this.video = new InputFile();
        this.video.setFilePath(file.getAbsolutePath());
        this.video.setFileName(file.getName());
    }

    public SendVideo(Long chatId, String fileName, File file) {
        this.chatId = chatId.toString();
        this.video = new InputFile();
        this.video.setFilePath(file.getAbsolutePath());
        this.video.setFileName(fileName);
    }

    public SendVideo(Long chatId, String fileId) {
        this.chatId = chatId.toString();
        this.video = new InputFile();
        this.video.setFileId(fileId);
    }

    public String getChatId() {
        return chatId;
    }

    public SendVideo setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public InputFile getVideo() {
        return video;
    }

    public SendVideo setVideo(String video) {
        this.video = new InputFile(video);
        return this;
    }

    public SendVideo setChatId(Long chatId) {
        Objects.requireNonNull(chatId);
        this.chatId = chatId.toString();
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public SendVideo setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public SendVideo setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
        return this;
    }

    public ReplyKeyboard getReplyMarkup() {
        return replyMarkup;
    }

    public SendVideo setReplyMarkup(ReplyKeyboard replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    public SendVideo setVideo(InputFile video) {
        Objects.requireNonNull(video, "video cannot be null!");
        this.video = video;
        return this;
    }

    public String getParseMode() {
        return parseMode;
    }

    public SendVideo setParseMode(String parseMode) {
        this.parseMode = parseMode;
        return this;
    }

    @Override
    public String toString() {
        return "SendVideo{" +
                "chatId='" + chatId + '\'' +
                ", video=" + video +
                ", caption='" + caption + '\'' +
                ", replyToMessageId=" + replyToMessageId +
                ", replyMarkup=" + replyMarkup +
                ", parseMode='" + parseMode + '\'' +
                '}';
    }
}

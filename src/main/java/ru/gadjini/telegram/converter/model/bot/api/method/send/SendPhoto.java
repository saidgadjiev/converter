package ru.gadjini.telegram.converter.model.bot.api.method.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.InputFile;

import java.io.File;

public class SendPhoto {

    public static final String METHOD = "sendphoto";

    public static final String CHATID_FIELD = "chat_id";

    public static final String PHOTO = "photo";

    public static final String CAPTION = "caption";

    @JsonProperty(CHATID_FIELD)
    private String chatId;

    @JsonProperty(PHOTO)
    private InputFile photo;

    @JsonProperty(CAPTION)
    private String caption;

    public SendPhoto(long chatId, String photoFileId) {
        this.chatId = String.valueOf(chatId);
        this.photo = new InputFile();
        this.photo.setFileId(photoFileId);
    }

    public SendPhoto(long chatId, File file) {
        this.chatId = String.valueOf(chatId);
        this.photo = new InputFile();
        this.photo.setFilePath(file.getAbsolutePath());
    }

    public String getChatId() {
        return chatId;
    }

    public InputFile getPhoto() {
        return photo;
    }

    public String getCaption() {
        return caption;
    }

    public SendPhoto setCaption(String caption) {
        this.caption = caption;

        return this;
    }
}

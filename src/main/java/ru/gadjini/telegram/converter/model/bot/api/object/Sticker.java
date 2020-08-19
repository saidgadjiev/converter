package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Sticker {

    private static final String FILEID_FIELD = "file_id";
    private static final String ISANIMATED_FIELD = "is_animated";
    private static final String FILESIZE_FIELD = "file_size";

    @JsonProperty(FILEID_FIELD)
    private String fileId;
    @JsonProperty(ISANIMATED_FIELD)
    private Boolean isAnimated;
    @JsonProperty(FILESIZE_FIELD)
    private Long fileSize;

    public String getFileId() {
        return fileId;
    }

    public Boolean getAnimated() {
        return isAnimated;
    }

    public Long getFileSize() {
        return fileSize;
    }

    @Override
    public String toString() {
        return "Sticker{" +
                "fileId='" + fileId + '\'' +
                ", isAnimated=" + isAnimated +
                '}';
    }
}

package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Audio {

    private static final String FILEID_FIELD = "file_id";
    private static final String MIMETYPE_FIELD = "mime_type";
    private static final String FILESIZE_FIELD = "file_size";
    private static final String FILE_NAME_FIELD = "file_name";
    private static final String THUMBNAIL_FIELD = "thumb";

    @JsonProperty(FILEID_FIELD)
    private String fileId;
    @JsonProperty(MIMETYPE_FIELD)
    private String mimeType;
    @JsonProperty(FILESIZE_FIELD)
    private Long fileSize;
    @JsonProperty(FILE_NAME_FIELD)
    private String fileName;
    @JsonProperty(THUMBNAIL_FIELD)
    private Thumb thumb;

    public String getFileId() {
        return fileId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Thumb getThumb() {
        return thumb;
    }

    public boolean hasThumb() {
        return thumb != null;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "Audio{" +
                "fileId='" + fileId + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }

}

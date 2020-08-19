package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Video {

    private static final String FILEID_FIELD = "file_id";
    private static final String MIMETYPE_FIELD = "mime_type";
    private static final String FILESIZE_FIELD = "file_size";
    private static final String FILENAME_FIELD = "file_name";
    private static final String THUMBNAIL = "thumb";

    @JsonProperty(FILEID_FIELD)
    private String fileId;
    @JsonProperty(MIMETYPE_FIELD)
    private String mimeType;
    @JsonProperty(FILESIZE_FIELD)
    private Long fileSize;
    @JsonProperty(FILENAME_FIELD)
    private String fileName;
    @JsonProperty(THUMBNAIL)
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

    public String getFileName() {
        return fileName;
    }

    public Thumb getThumb() {
        return thumb;
    }

    public boolean hasThumb() {
        return thumb != null;
    }

    @Override
    public String toString() {
        return "Video{" +
                "fileId='" + fileId + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}

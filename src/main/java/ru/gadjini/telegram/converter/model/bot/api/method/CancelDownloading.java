package ru.gadjini.telegram.converter.model.bot.api.method;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CancelDownloading {

    public static final String METHOD = "downloadcancel";

    @JsonProperty("file_id")
    private String fileId;

    public CancelDownloading(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }
}

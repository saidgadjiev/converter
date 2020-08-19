package ru.gadjini.telegram.converter.model.bot.api.method;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CancelUploading {

    public static final String METHOD = "uploadcancel";

    @JsonProperty("file_path")
    private String filePath;

    public CancelUploading(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}

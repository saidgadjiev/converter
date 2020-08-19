package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetFile {

    public static final String METHOD = "downloadfile";

    private static final String FILE_ID = "file_id";

    private static final String FILE_SIZE = "file_size";

    private static final String REMOVE_PARENT_DIR_ON_CANCEL = "remove_parent_dir_on_cancel";

    @JsonProperty(FILE_ID)
    private String fileId;

    private String path;

    @JsonProperty(REMOVE_PARENT_DIR_ON_CANCEL)
    private boolean removeParentDirOnCancel = false;

    @JsonProperty(FILE_SIZE)
    private long fileSize;

    private Progress progress;

    public GetFile() {}

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isRemoveParentDirOnCancel() {
        return removeParentDirOnCancel;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setRemoveParentDirOnCancel(boolean removeParentDirOnCancel) {
        this.removeParentDirOnCancel = removeParentDirOnCancel;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }
}

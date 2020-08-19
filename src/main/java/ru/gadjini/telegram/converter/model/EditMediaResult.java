package ru.gadjini.telegram.converter.model;

public class EditMediaResult {

    private String fileId;

    public EditMediaResult(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }
}

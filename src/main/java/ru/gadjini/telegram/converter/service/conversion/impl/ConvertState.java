package ru.gadjini.telegram.converter.service.conversion.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.LinkedHashSet;
import java.util.Set;

public class ConvertState {

    private String fileId;

    private String fileName;

    private String mimeType;

    private Long fileSize;

    private int messageId;

    private Format format;

    private String userLanguage;

    @JsonIgnore
    private Set<String> warnings = new LinkedHashSet<>();

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public void setUserLanguage(String userLanguage) {
        this.userLanguage = userLanguage;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void addWarn(String warn) {
        warnings.add(warn);
    }

    public Set<String> getWarnings() {
        return warnings;
    }

    public void deleteWarns() {
        warnings.clear();
    }

    public void setMedia(MessageMedia media) {
        setFileId(media.getFileId());
        setFileSize(media.getFileSize());
        setFileName(media.getFileName());
        setMimeType(media.getMimeType());
        setFormat(media.getFormat());
    }

    @Override
    public String toString() {
        return "ConvertState{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileSize=" + fileSize +
                ", messageId=" + messageId +
                ", format=" + format +
                ", userLanguage='" + userLanguage + '\'' +
                ", warnings=" + warnings +
                '}';
    }
}

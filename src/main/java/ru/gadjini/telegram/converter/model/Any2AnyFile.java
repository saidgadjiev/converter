package ru.gadjini.telegram.converter.model;

import ru.gadjini.telegram.converter.service.conversion.api.Format;

public class Any2AnyFile {

    private String fileId;

    private String fileName;

    private String mimeType;

    private Format format;

    private long fileSize;

    private String thumb;

    private String cachedFileId;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getCachedFileId() {
        return cachedFileId;
    }

    public void setCachedFileId(String cachedFileId) {
        this.cachedFileId = cachedFileId;
    }

    @Override
    public String toString() {
        return "Any2AnyFile{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", format=" + format +
                '}';
    }
}

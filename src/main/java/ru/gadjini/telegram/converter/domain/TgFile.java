package ru.gadjini.telegram.converter.domain;

import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

public class TgFile {

    public static final String TYPE = "tg_file";

    public static final String FILE_ID = "file_id";

    public static final String FILE_NAME = "file_name";

    public static final String MIME_TYPE = "mime_type";

    public static final String SIZE = "size";

    public static final String THUMB = "thumb";

    private String fileId;

    private String fileName;

    private String mimeType;

    private long size;

    private String thumb;

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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String sql() {
        StringBuilder sql = new StringBuilder("(\"");

        sql.append(fileId).append("\",");
        if (StringUtils.isNotBlank(mimeType)) {
            sql.append("\"").append(mimeType).append("\",");
        } else {
            sql.append(",");
        }
        if (StringUtils.isNotBlank(fileName)) {
            sql.append("\"").append(fileName).append("\",");
        } else {
            sql.append(",");
        }
        sql.append(size).append(",");
        if (StringUtils.isNotBlank(thumb)) {
            sql.append("\"").append(thumb).append("\"");
        }
        sql.append(")");

        return sql.toString();
    }

    public PGobject sqlObject() {
        PGobject pGobject = new PGobject();
        pGobject.setType(TYPE);
        try {
            pGobject.setValue(sql());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return pGobject;
    }
}

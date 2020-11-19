package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.io.File;

public class FileResult implements ConvertResult {

    private final SmartTempFile file;

    private final SmartTempFile thumb;

    private String fileName;

    public FileResult(String fileName, SmartTempFile file, SmartTempFile thumb) {
        this.fileName = fileName;
        this.file = file;
        this.thumb = thumb;
    }

    public File getThumb() {
        return thumb != null ? thumb.getFile() : null;
    }

    public File getFile() {
        return file.getFile();
    }

    @Override
    public ResultType resultType() {
        return ResultType.FILE;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public void close() {
        file.smartDelete();
        if (thumb != null) {
            thumb.smartDelete();
        }
    }
}

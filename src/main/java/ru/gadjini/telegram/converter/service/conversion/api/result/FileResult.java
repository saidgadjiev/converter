package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.io.File;

public class FileResult extends BaseConvertResult {

    private final SmartTempFile file;

    private String fileName;

    public FileResult(String fileName, SmartTempFile file, long time) {
        super(time);
        this.fileName = fileName;
        this.file = file;
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
    }
}

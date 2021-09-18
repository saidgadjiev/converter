package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;

public class FileResult implements ConversionResult {

    private SmartTempFile file;

    private final SmartTempFile thumb;

    private String fileName;

    private String caption;

    private Format format;

    public FileResult(String fileName, SmartTempFile file) {
        this(fileName, file, null, null, null);
    }

    public FileResult(String fileName, SmartTempFile file, SmartTempFile thumb) {
        this(fileName, file, thumb, null, null);
    }

    public FileResult(String fileName, SmartTempFile file, String caption) {
        this(fileName, file, null, caption);
    }

    public FileResult(String fileName, SmartTempFile file, SmartTempFile thumb, String caption) {
        this(fileName, file, thumb, caption, null);
    }

    public FileResult(String fileName, SmartTempFile file, SmartTempFile thumb, String caption, Format format) {
        this.fileName = fileName;
        this.file = file;
        this.thumb = thumb;
        this.caption = caption;
        this.format = format;
    }

    public File getThumb() {
        return thumb != null ? thumb.getFile() : null;
    }

    public File getFile() {
        return file.getFile();
    }

    public SmartTempFile getSmartFile() {
        return file;
    }

    public void setSmartFile(SmartTempFile file) {
        this.file = file;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Format getFormat() {
        return format;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public ResultType resultType() {
        return ResultType.FILE;
    }
}

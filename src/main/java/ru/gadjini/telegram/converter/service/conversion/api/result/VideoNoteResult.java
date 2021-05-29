package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;

public class VideoNoteResult implements ConversionResult {

    private Long duration;

    private SmartTempFile file;

    private String fileName;

    private Format format;

    public VideoNoteResult(String fileName, SmartTempFile file, Long duration, Format format) {
        this.fileName = fileName;
        this.file = file;
        this.duration = duration;
        this.format = format;
    }

    public Long getDuration() {
        return duration;
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile() {
        return file.getFile();
    }

    public Format getFormat() {
        return format;
    }

    @Override
    public ResultType resultType() {
        return ResultType.VIDEO_NOTE;
    }
}

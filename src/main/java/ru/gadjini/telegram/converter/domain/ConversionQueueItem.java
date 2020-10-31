package ru.gadjini.telegram.converter.domain;

import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;

public class ConversionQueueItem extends QueueItem {

    public static final String TYPE = "conversion_queue";

    public static final String EXCEPTION = "exception";

    public static final String TARGET_FORMAT = "target_format";

    public static final String MESSAGE = "message";

    public static final String FILES = "files";

    public static final String RESULT_FILE_ID = "result_file_id";

    private List<TgFile> files = new ArrayList<>();

    private Format targetFormat;

    private String message;

    private String resultFileId;

    public Format getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(Format targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TgFile> getFiles() {
        return files;
    }

    public void addFile(TgFile file) {
        this.files.add(file);
    }

    public void setFiles(List<TgFile> files) {
        this.files = files;
    }

    public TgFile getFirstFile() {
        return files.iterator().next();
    }

    public String getResultFileId() {
        return resultFileId;
    }

    public void setResultFileId(String resultFileId) {
        this.resultFileId = resultFileId;
    }

    public String getFirstFileId() {
        return getFirstFile().getFileId();
    }

    public Format getFirstFileFormat() {
        return getFirstFile().getFormat();
    }

    public String getFirstFileName() {
        return getFirstFile().getFileName();
    }

    @Override
    public long getSize() {
        return files.stream().mapToLong(TgFile::getSize).sum();
    }
}

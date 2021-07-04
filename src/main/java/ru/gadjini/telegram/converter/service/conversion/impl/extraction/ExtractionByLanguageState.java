package ru.gadjini.telegram.converter.service.conversion.impl.extraction;

import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class ExtractionByLanguageState {

    private int replyToMessageId;

    private int userId;

    private String filePath;

    private TgFile file;

    private Format targetFormat;

    private int progressMessageId;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public TgFile getFile() {
        return file;
    }

    public void setFile(TgFile file) {
        this.file = file;
    }

    public Format getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(Format targetFormat) {
        this.targetFormat = targetFormat;
    }

    public int getProgressMessageId() {
        return progressMessageId;
    }

    public void setProgressMessageId(int progressMessageId) {
        this.progressMessageId = progressMessageId;
    }
}

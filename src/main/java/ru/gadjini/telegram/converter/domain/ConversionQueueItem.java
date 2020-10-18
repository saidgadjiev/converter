package ru.gadjini.telegram.converter.domain;

import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.domain.TgUser;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConversionQueueItem {

    public static final String TYPE = "conversion_queue";

    public static final String EXCEPTION = "exception";

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String REPLY_TO_MESSAGE_ID = "reply_to_message_id";

    public static final String CREATED_AT = "created_at";

    public static final String STARTED_AT = "started_at";

    public static final String COMPLETED_AT = "completed_at";

    public static final String LAST_RUN_AT = "last_run_at";

    public static final String STATUS = "status";

    public static final String TARGET_FORMAT = "target_format";

    public static final String PLACE_IN_QUEUE = "place_in_queue";

    public static final String MESSAGE = "message";

    public static final String PROGRESS_MESSAGE_ID = "progress_message_id";

    public static final String FILES = "files";

    public static final String SUPPRESS_USER_EXCEPTIONS = "suppress_user_exceptions";

    public static final String RESULT_FILE_ID = "result_file_id";

    private int id;

    private ZonedDateTime createdAt;

    private ZonedDateTime statedAt;

    private ZonedDateTime lastRunAt;

    private ZonedDateTime completedAt;

    private int userId;

    private int progressMessageId;

    private TgUser user;

    private int replyToMessageId;

    private List<TgFile> files = new ArrayList<>();

    private int placeInQueue;

    private Format targetFormat;

    private Status status;

    private String message;

    private boolean suppressUserExceptions;

    private String resultFileId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getStatedAt() {
        return statedAt;
    }

    public void setStatedAt(ZonedDateTime statedAt) {
        this.statedAt = statedAt;
    }

    public ZonedDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(ZonedDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(ZonedDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getFirstFileId() {
        return files.iterator().next().getFileId();
    }

    public String getFirstFileName() {
        return files.iterator().next().getFileName();
    }

    public String getFirstFileMimeType() {
        return files.iterator().next().getMimeType();
    }

    public Format getFirstFileFormat() {
        return files.iterator().next().getFormat();
    }

    public long getSize() {
        return files.stream().mapToLong(TgFile::getSize).sum();
    }

    public int getPlaceInQueue() {
        return placeInQueue;
    }

    public void setPlaceInQueue(int placeInQueue) {
        this.placeInQueue = placeInQueue;
    }

    public Format getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(Format targetFormat) {
        this.targetFormat = targetFormat;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TgUser getUser() {
        return user;
    }

    public void setUser(TgUser user) {
        this.user = user;
    }

    public int getProgressMessageId() {
        return progressMessageId;
    }

    public void setProgressMessageId(int progressMessageId) {
        this.progressMessageId = progressMessageId;
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

    public boolean isSuppressUserExceptions() {
        return suppressUserExceptions;
    }

    public void setSuppressUserExceptions(boolean suppressUserExceptions) {
        this.suppressUserExceptions = suppressUserExceptions;
    }

    public enum Status {

        WAITING(0),

        PROCESSING(1),

        EXCEPTION(2),

        COMPLETED(3),

        CANDIDATE_NOT_FOUND(4),

        BLOCKED(5);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Status fromCode(int code) {
            for (Status status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown queue item status " + code);
        }
    }
}

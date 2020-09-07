package ru.gadjini.telegram.converter.domain;

import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.domain.TgUser;

import java.time.ZonedDateTime;

public class ConversionQueueItem {

    public static final String TYPE = "conversion_queue";

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String REPLY_TO_MESSAGE_ID = "reply_to_message_id";

    public static final String FILE_ID = "file_id";

    public static final String FILE_NAME = "file_name";

    public static final String MIME_TYPE = "mime_type";

    public static final String FORMAT = "format";

    public static final String SIZE = "size";

    public static final String CREATED_AT = "created_at";

    public static final String LAST_RUN_AT = "last_run_at";

    public static final String STATUS = "status";

    public static final String TARGET_FORMAT = "target_format";

    public static final String PLACE_IN_QUEUE = "place_in_queue";

    public static final String MESSAGE = "message";

    private int id;

    private ZonedDateTime createdAt;

    private ZonedDateTime statedAt;

    private ZonedDateTime lastRunAt;

    private ZonedDateTime completedAt;

    private int userId;

    private TgUser user;

    private int replyToMessageId;

    private String fileId;

    private String fileName;

    private String mimeType;

    private Format format;

    private long size;

    private int placeInQueue;

    private Format targetFormat;

    private Status status;

    private String message;

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

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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

    public enum Status {

        WAITING(0),

        PROCESSING(1),

        EXCEPTION(2),

        COMPLETED(3),

        CANDIDATE_NOT_FOUND(4);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Status fromCode(int code) {
            for (Status status: values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown queue item status " + code);
        }
    }
}

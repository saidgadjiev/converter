package ru.gadjini.telegram.converter.service.file;

import java.time.LocalDateTime;

public class InputFileState {

    public static final String REPLY_TO_MESSAGE_ID = "replyToMessageId";

    public static final String STATE = "state";

    private Integer replyToMessageId;

    private String fileId;

    private String command;

    private LocalDateTime createdAt = LocalDateTime.now();

    private State state = State.PENDING;

    public InputFileState(Integer replyToMessageId, String fileId, String command) {
        this.replyToMessageId = replyToMessageId;
        this.fileId = fileId;
        this.command = command;
    }

    public InputFileState() {}

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public enum State {

        PENDING,

        PROCESSING,

        COMPLETED
    }
}

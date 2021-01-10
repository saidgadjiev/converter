package ru.gadjini.telegram.converter.command.keyboard.start;

public class SettingsState {

    private int messageId;

    private String bitrate;

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
}

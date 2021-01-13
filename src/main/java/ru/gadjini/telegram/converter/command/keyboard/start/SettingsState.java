package ru.gadjini.telegram.converter.command.keyboard.start;

import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class SettingsState {

    private int messageId;

    private String bitrate;

    private Format targetFormat;

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

    public Format getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(Format targetFormat) {
        this.targetFormat = targetFormat;
    }
}

package ru.gadjini.telegram.converter.command.keyboard.start;

import org.apache.commons.lang3.StringUtils;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class SettingsState {

    private int messageId;

    private String bitrate;

    private String resolution;

    private String frequency;

    private Format format;

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

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Format getFormat() {
        return format;
    }

    public Format getFormatOrDefault(Format defaultFormat) {
        return format == null ? defaultFormat : format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getFrequencyOrDefault(String defaultFrequency) {
        return StringUtils.isBlank(frequency) ? frequency : defaultFrequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
}

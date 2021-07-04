package ru.gadjini.telegram.converter.command.keyboard.start;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class SettingsState {

    private int messageId;

    private String bitrate;

    private String resolution;

    private String frequency;

    private Format format;

    private Period cutStartPoint;

    private Period cutEndPoint;

    private String languageToExtract;

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
        return StringUtils.isBlank(frequency) ? defaultFrequency : frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Period getCutStartPoint() {
        return cutStartPoint;
    }

    public void setCutStartPoint(Period cutStartPoint) {
        this.cutStartPoint = cutStartPoint;
    }

    public Period getCutEndPoint() {
        return cutEndPoint;
    }

    public void setCutEndPoint(Period cutEndPoint) {
        this.cutEndPoint = cutEndPoint;
    }

    public String getLanguageToExtract() {
        return languageToExtract;
    }

    public void setLanguageToExtract(String languageToExtract) {
        this.languageToExtract = languageToExtract;
    }
}

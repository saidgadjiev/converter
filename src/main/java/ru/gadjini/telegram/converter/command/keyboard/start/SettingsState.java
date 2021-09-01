package ru.gadjini.telegram.converter.command.keyboard.start;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class SettingsState {

    private Integer messageId;

    private String bitrate;

    private String resolution;

    private String crf;

    private String frequency;

    private Format format;

    private Period cutStartPoint;

    private Period cutEndPoint;

    private String languageToExtract;

    private String vavMergeAudioMode;

    private String vavMergeSubtitlesMode;

    private String audioCodec;

    private String audioBitrate;

    private String audioMonoStereo;

    private String audioChannelLayout;

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
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

    public String getVavMergeSubtitlesMode() {
        return vavMergeSubtitlesMode;
    }

    public void setVavMergeSubtitlesMode(String vavMergeSubtitlesMode) {
        this.vavMergeSubtitlesMode = vavMergeSubtitlesMode;
    }

    public String getVavMergeAudioMode() {
        return vavMergeAudioMode;
    }

    public void setVavMergeAudioMode(String vavMergeAudioMode) {
        this.vavMergeAudioMode = vavMergeAudioMode;
    }

    public String getCrf() {
        return crf;
    }

    public void setCrf(String crf) {
        this.crf = crf;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(String audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public String getAudioMonoStereo() {
        return audioMonoStereo;
    }

    public void setAudioMonoStereo(String audioMonoStereo) {
        this.audioMonoStereo = audioMonoStereo;
    }

    public String getAudioChannelLayout() {
        return audioChannelLayout;
    }

    public void setAudioChannelLayout(String audioChannelLayout) {
        this.audioChannelLayout = audioChannelLayout;
    }
}

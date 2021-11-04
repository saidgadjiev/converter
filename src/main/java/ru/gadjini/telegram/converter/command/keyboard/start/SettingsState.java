package ru.gadjini.telegram.converter.command.keyboard.start;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioBitrateState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoQualityState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.utils.BitrateUtils;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class SettingsState {

    private Integer messageId;

    private String bitrate;

    private String resolution;

    private String frequency;

    private Format format;

    private Period cutStartPoint;

    private Period cutEndPoint;

    private String languageToExtract;

    private String vavMergeAudioMode;

    private String vavMergeSubtitlesMode;

    private String audioCodec;

    private String audioBitrate;

    private int videoBitrate;

    private String compressBy;

    private String audioChannelLayout;

    private String bassBoost;

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

    public int getResolutionOrDefault(int defaultResolution) {
        return EditVideoResolutionState.AUTO.equals(resolution) ? defaultResolution : Integer.parseInt(resolution);
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

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getAudioBitrate() {
        return audioBitrate;
    }

    public String getAudioBitrateInKBytes() {
        return EditVideoAudioBitrateState.AUTO.equals(audioBitrate) ? EditVideoAudioBitrateState.AUTO : String.valueOf(BitrateUtils.toKBytes(Integer.parseInt(audioBitrate)));
    }

    public int getParsedAudioBitrate() {
        return EditVideoAudioBitrateState.AUTO.equals(audioBitrate) ? 0 : Integer.parseInt(audioBitrate);
    }

    public void setAudioBitrate(String audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public String getAudioChannelLayout() {
        return audioChannelLayout;
    }

    public void setAudioChannelLayout(String audioChannelLayout) {
        this.audioChannelLayout = audioChannelLayout;
    }

    public String getBassBoost() {
        return bassBoost;
    }

    public void setBassBoost(String bassBoost) {
        this.bassBoost = bassBoost;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public String getCompressBy() {
        return compressBy;
    }

    public int getParsedCompressBy() {
        return EditVideoQualityState.AUTO.equals(compressBy) ? 0 : Integer.parseInt(compressBy);
    }

    public void setCompressBy(String compressBy) {
        this.compressBy = compressBy;
    }
}

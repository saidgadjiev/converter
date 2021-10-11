package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public class EditVideoState {

    private EditVideoSettingsStateName stateName;

    private Integer currentVideoResolution;

    private Integer currentVideoBitrate;

    private String downloadedFilePath;

    private ConvertState state;

    public ConvertState getState() {
        return state;
    }

    public void setState(ConvertState state) {
        this.state = state;
    }

    public EditVideoSettingsStateName getStateName() {
        return stateName;
    }

    public void setStateName(EditVideoSettingsStateName stateName) {
        this.stateName = stateName;
    }

    public int getMessageId() {
        return state.getMessageId();
    }

    public void setMessageId(int messageId) {
        state.setMessageId(messageId);
    }

    public String getUserLanguage() {
        return state.getUserLanguage();
    }

    public void setUserLanguage(String userLanguage) {
        state.setUserLanguage(userLanguage);
    }

    public void addMedia(MessageMedia media) {
        state.addMedia(media);
    }

    public void setMedia(MessageMedia media) {
        state.setMedia(media);
    }

    public void setMedia(int index, MessageMedia firstFile) {
        state.setMedia(index, firstFile);
    }

    public MessageMedia getMedia(int index) {
        return state.getMedia(index);
    }

    public List<MessageMedia> getFiles() {
        return state.getFiles();
    }

    public MessageMedia getFirstFile() {
        return state.getFirstFile();
    }

    public Format getFirstFormat() {
        return state.getFirstFormat();
    }

    public void clearMedia() {
        state.clearMedia();
    }

    public boolean isTextAppendedMessageSent() {
        return state.isTextAppendedMessageSent();
    }

    public void setTextAppendedMessageSent(boolean textAppendedMessageSent) {
        state.setTextAppendedMessageSent(textAppendedMessageSent);
    }

    public Format getMultiMediaFormat() {
        return state.getMultiMediaFormat();
    }

    public void setMultiMediaFormat(Format multiMediaFormat) {
        state.setMultiMediaFormat(multiMediaFormat);
    }

    public void setSettings(SettingsState settings) {
        state.setSettings(settings);
    }

    public SettingsState getSettings() {
        return state.getSettings();
    }

    public Integer getCurrentVideoResolution() {
        return currentVideoResolution;
    }

    public void setCurrentVideoResolution(Integer currentVideoResolution) {
        this.currentVideoResolution = currentVideoResolution;
    }

    public Integer getCurrentVideoBitrate() {
        return currentVideoBitrate;
    }

    public void setCurrentVideoBitrate(Integer currentVideoBitrate) {
        this.currentVideoBitrate = currentVideoBitrate;
    }

    public String getDownloadedFilePath() {
        return downloadedFilePath;
    }

    public void setDownloadedFilePath(String downloadedFilePath) {
        this.downloadedFilePath = downloadedFilePath;
    }

}

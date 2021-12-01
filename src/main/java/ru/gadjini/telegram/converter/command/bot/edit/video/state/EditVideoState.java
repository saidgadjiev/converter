package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;

import java.util.List;

public class EditVideoState {

    private EditVideoSettingsStateName stateName;

    private int currentVideoResolution;

    private boolean hasAudio;

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

    public void setSettings(SettingsState settings) {
        state.setSettings(settings);
    }

    public SettingsState getSettings() {
        return state.getSettings();
    }

    public int getCurrentVideoResolution() {
        return currentVideoResolution;
    }

    public void setCurrentVideoResolution(int currentVideoResolution) {
        this.currentVideoResolution = currentVideoResolution;
    }

    public String getDownloadedFilePath() {
        return downloadedFilePath;
    }

    public void setDownloadedFilePath(String downloadedFilePath) {
        this.downloadedFilePath = downloadedFilePath;
    }

    public boolean hasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }
}

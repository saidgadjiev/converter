package ru.gadjini.telegram.converter.command.keyboard.start;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConvertState {

    private int messageId;

    private List<MessageMedia> files = new ArrayList<>();

    private String userLanguage;

    private Format multiMediaFormat;

    private boolean textAppendedMessageSent = false;

    private SettingsState settings;

    public ConvertState() {
    }

    public ConvertState(int filesSize) {
        files = Arrays.asList(new MessageMedia[filesSize]);
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public void setUserLanguage(String userLanguage) {
        this.userLanguage = userLanguage;
    }

    public void addMedia(MessageMedia media) {
        files.add(media);
    }

    public void setMedia(MessageMedia media) {
        files.clear();
        files.add(media);
    }

    public void setMedia(int index, MessageMedia firstFile) {
        files.set(index, firstFile);
    }

    public MessageMedia getMedia(int index) {
        return files.get(index);
    }

    public List<MessageMedia> getFiles() {
        return files;
    }

    @JsonIgnore
    public MessageMedia getFirstFile() {
        return files.isEmpty() ? null : files.iterator().next();
    }

    @JsonIgnore
    public Format getFirstFormat() {
        return files.isEmpty() ? null : getFirstFile().getFormat();
    }

    public void clearMedia() {
        files.clear();
    }

    @JsonIgnore
    public boolean isTextAppendedMessageSent() {
        return textAppendedMessageSent;
    }

    public void setTextAppendedMessageSent(boolean textAppendedMessageSent) {
        this.textAppendedMessageSent = textAppendedMessageSent;
    }

    @JsonIgnore
    public Format getMultiMediaFormat() {
        return multiMediaFormat;
    }

    public void setMultiMediaFormat(Format multiMediaFormat) {
        this.multiMediaFormat = multiMediaFormat;
    }

    public void setSettings(SettingsState settings) {
        this.settings = settings;
    }

    public SettingsState getSettings() {
        return settings;
    }
}

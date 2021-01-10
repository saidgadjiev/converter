package ru.gadjini.telegram.converter.command.keyboard.start;

import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;

public class ConvertState {

    private int messageId;

    private List<MessageMedia> files = new ArrayList<>();

    private String userLanguage;

    private Format multiMediaFormat;

    private boolean textAppendedMessageSent = false;

    private SettingsState settings;

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

    public List<MessageMedia> getFiles() {
        return files;
    }

    public MessageMedia getFirstFile() {
        return files.isEmpty() ? null : files.iterator().next();
    }

    public Format getFirstFormat() {
        return files.isEmpty() ? null : getFirstFile().getFormat();
    }

    public boolean isTextAppendedMessageSent() {
        return textAppendedMessageSent;
    }

    public void setTextAppendedMessageSent(boolean textAppendedMessageSent) {
        this.textAppendedMessageSent = textAppendedMessageSent;
    }

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

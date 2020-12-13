package ru.gadjini.telegram.converter.service.conversion.impl;

import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConvertState {

    private int messageId;

    private List<MessageMedia> files = new ArrayList<>();

    private String userLanguage;

    private Set<String> warnings = new LinkedHashSet<>();

    private boolean textAppendedMessageSent = false;

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

    public void addWarn(String warn) {
        warnings.add(warn);
    }

    public Set<String> getWarnings() {
        return warnings;
    }

    public void deleteWarns() {
        warnings.clear();
    }

    public void addMedia(MessageMedia media) {
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

    @Override
    public String toString() {
        return "ConvertState{" +
                ", messageId=" + messageId +
                ", userLanguage='" + userLanguage + '\'' +
                ", warnings=" + warnings +
                '}';
    }
}

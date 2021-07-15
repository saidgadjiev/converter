package ru.gadjini.telegram.converter.command.bot.vavmerge;

import com.google.common.collect.EvictingQueue;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;

public class VavMergeState {

    private int messageId;

    private String userLanguage;

    private MessageMedia video;

    private EvictingQueue<MessageMedia> audios;

    private EvictingQueue<MessageMedia> subtitles;

    private String audioMode;

    private String subtitlesMode;

    public VavMergeState() {

    }

    public VavMergeState(int audiosMaxSize, int subtitlesMaxSize) {
        audios = EvictingQueue.create(audiosMaxSize);
        subtitles = EvictingQueue.create(subtitlesMaxSize);
    }

    public MessageMedia getVideo() {
        return video;
    }

    public void setVideo(MessageMedia video) {
        this.video = video;
    }

    public void addAudio(MessageMedia audio) {
        this.audios.add(audio);
    }

    public void addSubtitles(MessageMedia subtitles) {
        this.subtitles.add(subtitles);
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public void setUserLanguage(String userLanguage) {
        this.userLanguage = userLanguage;
    }

    public EvictingQueue<MessageMedia> getAudios() {
        return audios;
    }

    public EvictingQueue<MessageMedia> getSubtitles() {
        return subtitles;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getSubtitlesMode() {
        return subtitlesMode;
    }

    public void setSubtitlesMode(String subtitlesMode) {
        this.subtitlesMode = subtitlesMode;
    }

    public String getAudioMode() {
        return audioMode;
    }

    public void setAudioMode(String audioMode) {
        this.audioMode = audioMode;
    }
}

package ru.gadjini.telegram.converter.domain.watermark.audio;

import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;

public class AudioWatermark {

    public static final String USER_ID = "user_id";

    public static final String AUDIO = "audio";

    private long userId;

    private TgFile audio;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public TgFile getAudio() {
        return audio;
    }

    public void setAudio(TgFile audio) {
        this.audio = audio;
    }
}

package ru.gadjini.telegram.converter.command.bot.watermark.audio.settings;

import ru.gadjini.telegram.converter.command.bot.watermark.audio.state.AudioWatermarkStateName;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;

public class AudioWatermarkSettings {

    private AudioWatermarkStateName stateName;

    private MessageMedia audio;

    public MessageMedia getAudio() {
        return audio;
    }

    public void setAudio(MessageMedia audio) {
        this.audio = audio;
    }

    public AudioWatermarkStateName getStateName() {
        return stateName;
    }

    public void setStateName(AudioWatermarkStateName stateName) {
        this.stateName = stateName;
    }
}

package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

public class AudioResult extends FileResult {

    private String audioPerformer;

    private String audioTitle;

    private Integer duration;

    public AudioResult(String fileName, SmartTempFile file, String audioPerformer, String audioTitle, SmartTempFile thumb, Integer duration) {
        super(fileName, file, thumb);
        this.audioPerformer = audioPerformer;
        this.audioTitle = audioTitle;
        this.duration = duration;
    }

    @Override
    public ResultType resultType() {
        return ResultType.AUDIO;
    }

    public String getAudioPerformer() {
        return audioPerformer;
    }

    public String getAudioTitle() {
        return audioTitle;
    }

    public Integer getDuration() {
        return duration;
    }
}

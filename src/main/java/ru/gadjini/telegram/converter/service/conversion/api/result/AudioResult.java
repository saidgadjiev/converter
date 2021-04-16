package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

public class AudioResult extends FileResult {

    private String audioPerformer;

    private String audioTitle;

    private Integer duration;

    public AudioResult(String fileName, SmartTempFile file, String audioPerformer, String audioTitle, SmartTempFile thumb, Integer duration) {
        this(fileName, file, audioPerformer, audioTitle, thumb, duration, null);
    }

    public AudioResult(String fileName, SmartTempFile file, String audioPerformer, String audioTitle, SmartTempFile thumb, Integer duration, String caption) {
        super(fileName, file, thumb, caption);
        this.audioPerformer = audioPerformer;
        this.audioTitle = audioTitle;
        this.duration = duration;
    }

    public AudioResult(String fileName, SmartTempFile file, Integer duration, String caption) {
        this(fileName, file, null, null, null, duration, caption);
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

package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

public class VoiceResult extends FileResult {

    private final Integer duration;

    public VoiceResult(String fileName, SmartTempFile file, Integer duration) {
        this(fileName, file, duration, null);
    }

    public VoiceResult(String fileName, SmartTempFile file, Integer duration, String caption) {
        super(fileName, file, null, caption);
        this.duration = duration;
    }

    public Integer getDuration() {
        return duration;
    }

    @Override
    public ResultType resultType() {
        return ResultType.VOICE;
    }
}

package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.io.File;

public class AudioResult extends FileResult {

    private String audioPerformer;

    private String audioTitle;

    private SmartTempFile thumb;

    public AudioResult(String fileName, SmartTempFile file, String audioPerformer, String audioTitle, SmartTempFile thumb) {
        super(fileName, file);
        this.audioPerformer = audioPerformer;
        this.audioTitle = audioTitle;
        this.thumb = thumb;
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

    public File getThumb() {
        return thumb != null ? thumb.getFile() : null;
    }

    @Override
    public void close() {
        super.close();
        if (thumb != null) {
            thumb.smartDelete();
        }
    }
}

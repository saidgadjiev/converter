package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class VideoResult extends FileResult {

    private Integer width;
    private Integer height;
    private Integer duration;
    private boolean supportsStreaming;

    public VideoResult(String fileName, SmartTempFile file, Format format, SmartTempFile thumb, Integer width,
                       Integer height, Integer duration, boolean supportsStreaming) {
        super(fileName, file, thumb, format);
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.supportsStreaming = supportsStreaming;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getDuration() {
        return duration;
    }

    public boolean isSupportsStreaming() {
        return supportsStreaming;
    }

    @Override
    public ResultType resultType() {
        return ResultType.VIDEO;
    }
}

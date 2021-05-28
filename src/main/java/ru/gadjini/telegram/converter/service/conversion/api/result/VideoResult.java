package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class VideoResult extends FileResult {

    private Integer width;
    private Integer height;
    private Long duration;
    private boolean supportsStreaming;

    public VideoResult(String fileName, SmartTempFile file, Format format, SmartTempFile thumb, Integer width,
                       Integer height, Long duration, boolean supportsStreaming) {
        this(fileName, file, format, thumb, width, height, duration, supportsStreaming, null);
    }

    public VideoResult(String fileName, SmartTempFile file, Format format, Integer width,
                       Integer height, Long duration, boolean supportsStreaming) {
        this(fileName, file, format, null, width, height, duration, supportsStreaming, null);
    }

    public VideoResult(String fileName, SmartTempFile file, Format format, SmartTempFile thumb, Integer width,
                       Integer height, Long duration, boolean supportsStreaming, String caption) {
        super(fileName, file, thumb, caption, format);
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

    public Long getDuration() {
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

package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

public class VideoResult extends FileResult {

    private Integer width;
    private Integer height;
    private Integer duration;

    public VideoResult(String fileName, SmartTempFile file, SmartTempFile thumb, Integer width, Integer height, Integer duration) {
        super(fileName, file, thumb);
        this.width = width;
        this.height = height;
        this.duration = duration;
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

    @Override
    public ResultType resultType() {
        return ResultType.VIDEO;
    }
}

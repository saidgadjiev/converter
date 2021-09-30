package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class AnimationResult extends VideoResult {

    public AnimationResult(String fileName, SmartTempFile file, Format format, SmartTempFile thumb,
                           Integer width, Integer height, Long duration, boolean supportsStreaming) {
        super(fileName, file, format, thumb, width, height, duration, supportsStreaming);
    }

    public AnimationResult(String fileName, SmartTempFile file, Format format, Integer width,
                           Integer height, Long duration, boolean supportsStreaming) {
        super(fileName, file, format, width, height, duration, supportsStreaming);
    }

    public AnimationResult(String fileName, SmartTempFile file, Format format, SmartTempFile thumb,
                           Integer width, Integer height, Long duration, boolean supportsStreaming, String caption) {
        super(fileName, file, format, thumb, width, height, duration, supportsStreaming, caption);
    }

    @Override
    public ResultType resultType() {
        return ResultType.ANIMATION;
    }
}

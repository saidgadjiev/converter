package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.converter.io.SmartTempFile;

public class StickerResult extends FileResult {

    public StickerResult(SmartTempFile file, long time) {
        super(null, file, time);
    }

    public ResultType resultType() {
        return ResultType.STICKER;
    }
}

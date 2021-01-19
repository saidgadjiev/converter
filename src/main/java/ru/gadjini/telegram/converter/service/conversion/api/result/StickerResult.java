package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

public class StickerResult extends FileResult {

    public StickerResult(String fileName, SmartTempFile file) {
        super(fileName, file);
    }

    public ResultType resultType() {
        return ResultType.STICKER;
    }
}

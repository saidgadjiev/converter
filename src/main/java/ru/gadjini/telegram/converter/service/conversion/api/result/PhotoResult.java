package ru.gadjini.telegram.converter.service.conversion.api.result;

import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class PhotoResult extends FileResult {
    public PhotoResult(String fileName, SmartTempFile file) {
        super(fileName, file);
    }

    public PhotoResult(String fileName, SmartTempFile file, SmartTempFile thumb) {
        super(fileName, file, thumb);
    }

    public PhotoResult(String fileName, SmartTempFile file, String caption) {
        super(fileName, file, caption);
    }

    public PhotoResult(String fileName, SmartTempFile file, SmartTempFile thumb, String caption) {
        super(fileName, file, thumb, caption);
    }

    public PhotoResult(String fileName, SmartTempFile file, SmartTempFile thumb, String caption, Format format) {
        super(fileName, file, thumb, caption, format);
    }

    @Override
    public ResultType resultType() {
        return ResultType.PHOTO;
    }
}

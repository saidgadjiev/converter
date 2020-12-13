package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class RenameFormatsConverter extends BaseAny2AnyConverter {

    private static final String TAG = "rename2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.XML), List.of(Format.TXT)
    );

    public RenameFormatsConverter() {
        super(MAP);
    }

    @Override
    protected ConvertResult doConvert(ConversionQueueItem conversionQueueItem) {
        SmartTempFile src = conversionQueueItem.getDownloadedFile(conversionQueueItem.getFirstFileId());

        try {
            SmartTempFile out = getFileService().createTempFile(conversionQueueItem.getUserId(), conversionQueueItem.getFirstFileId(), TAG, conversionQueueItem.getTargetFormat().getExt());

            try {
                src.renameTo(out.getFile());

                String fileName = Any2AnyFileNameUtils.getFileName(conversionQueueItem.getFirstFileName(), conversionQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, out);
            } catch (Throwable e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            src.smartDelete();
        }
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Excel2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "excel2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.XLS, Format.XLSX), List.of(Format.PDF));

    @Autowired
    public Excel2AnyConverter() {
        super(MAP);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            Workbook workbook = new Workbook(file.getAbsolutePath());
            try {
                SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                try {
                    workbook.save(out.getAbsolutePath(), SaveFormat.PDF);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.PDF.getExt());
                    return new FileResult(fileName, out, null);
                } catch (Throwable e) {
                    out.smartDelete();
                    throw e;
                }
            } finally {
                workbook.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

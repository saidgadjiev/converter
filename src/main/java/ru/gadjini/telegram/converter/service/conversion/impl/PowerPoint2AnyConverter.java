package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
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
public class PowerPoint2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "pp2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PPTX,
            Format.PPT,
            Format.PPTM,
            Format.POTX,
            Format.POT,
            Format.POTM,
            Format.PPS,
            Format.PPSX,
            Format.PPSM), List.of(Format.PDF));

    @Autowired
    public PowerPoint2AnyConverter() {
        super(MAP);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            Presentation presentation = new Presentation(file.getAbsolutePath());
            try {
                SmartTempFile tempFile = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
                try {
                    presentation.save(tempFile.getAbsolutePath(), SaveFormat.Pdf);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.PDF.getExt());
                    return new FileResult(fileName, tempFile, null);
                } catch (Throwable e) {
                    tempFile.smartDelete();
                    throw e;
                }
            } finally {
                presentation.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class PowerPoint2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "pp2";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>();

    static {
        MAP.put(List.of(PPTX), List.of(PDF));
        MAP.put(List.of(PPT), List.of(PDF, PPS));
        MAP.put(List.of(PPTM), List.of(PDF));
        MAP.put(List.of(POTX), List.of(PDF));
        MAP.put(List.of(POT), List.of(PDF));
        MAP.put(List.of(POTM), List.of(PDF));
        MAP.put(List.of(PPS), List.of(PDF));
        MAP.put(List.of(PPSX), List.of(PDF));
        MAP.put(List.of(PPSM), List.of(PDF));
    }

    @Autowired
    public PowerPoint2AnyConverter() {
        super(MAP);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            Presentation presentation = new Presentation(file.getAbsolutePath());
            try {
                SmartTempFile tempFile = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    presentation.save(tempFile.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
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

    private int getSaveFormat(Format format) {
        switch (format) {
            case PDF:
                return SaveFormat.Pdf;
            case PPS:
                return SaveFormat.Pps;
        }

        throw new IllegalArgumentException("Save format not found for " + format);
    }
}

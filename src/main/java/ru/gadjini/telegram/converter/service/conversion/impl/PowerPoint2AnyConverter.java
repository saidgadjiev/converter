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
import ru.gadjini.telegram.converter.utils.FormatMapUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class PowerPoint2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "pp2";

    private static final Map<List<Format>, List<Format>> MAP;

    static {
        Set<Format> asposeSlidesSaveFormats = Set.of(HTML, ODP, OTP, PDF, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS);
        Set<Format> asposeSlidesLoadFormats = Set.of(ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX);

        MAP = FormatMapUtils.buildMap(asposeSlidesLoadFormats, asposeSlidesSaveFormats);
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
                    return new FileResult(fileName, tempFile);
                } catch (Throwable e) {
                    tempFile.smartDelete();
                    throw e;
                }
            } finally {
                presentation.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case PDF:
                return SaveFormat.Pdf;
            case PPS:
                return SaveFormat.Pps;
            case PPT:
                return SaveFormat.Ppt;
            case PPTX:
                return SaveFormat.Pptx;
            case POT:
                return SaveFormat.Pot;
            case POTX:
                return SaveFormat.Potx;
            case POTM:
                return SaveFormat.Potm;
            case PPSX:
                return SaveFormat.Ppsx;
            case PPSM:
                return SaveFormat.Ppsm;
            case ODP:
                return SaveFormat.Odp;
            case PPTM:
                return SaveFormat.Pptm;
            case XPS:
                return SaveFormat.Xps;
            case TIFF:
                return SaveFormat.Tiff;
            case HTML:
                return SaveFormat.Html;
            case SWF:
                return SaveFormat.Swf;
            case OTP:
                return SaveFormat.Otp;
        }

        throw new IllegalArgumentException("Save format not found for " + format);
    }
}

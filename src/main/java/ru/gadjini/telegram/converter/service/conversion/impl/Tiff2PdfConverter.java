package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.Image2PdfDevice;
import ru.gadjini.telegram.converter.service.image.device.ImageMagickDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
@SuppressWarnings("CPD-START")
public class Tiff2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "tiff2pdf";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(TIFF), List.of(PDF, PDF_LOSSLESS)
    );

    private final Image2PdfDevice image2PdfDevice;

    private final ImageMagickDevice imageMagickDevice;

    @Autowired
    public Tiff2PdfConverter(Image2PdfDevice image2PdfDevice, ImageMagickDevice imageMagickDevice) {
        super(MAP);
        this.image2PdfDevice = image2PdfDevice;
        this.imageMagickDevice = imageMagickDevice;
    }

    @Override
    public FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, PDF.getExt());
            try {
                if (fileQueueItem.getTargetFormat() == PDF_LOSSLESS) {
                    image2PdfDevice.convert2Pdf(file.getAbsolutePath(), result.getAbsolutePath(), FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));
                } else {
                    imageMagickDevice.convert2Pdf(file.getAbsolutePath(), result.getAbsolutePath(), FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));
                }

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, result);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw e;
            }
        } catch (ProcessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}

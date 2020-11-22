package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.Image2PdfDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.PDF;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.TIFF;

@Component
@SuppressWarnings("CPD-START")
public class Tiff2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "tiff2pdf";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(TIFF), List.of(PDF)
    );

    private final Image2PdfDevice image2PdfDevice;

    @Autowired
    public Tiff2PdfConverter(Image2PdfDevice image2PdfDevice) {
        super(MAP);
        this.image2PdfDevice = image2PdfDevice;
    }

    @Override
    public FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            SmartTempFile tempFile = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, PDF.getExt());
            try {
                image2PdfDevice.convert2Pdf(file.getAbsolutePath(), tempFile.getAbsolutePath(), FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, tempFile, null);
            } catch (Throwable e) {
                tempFile.smartDelete();
                throw e;
            }
        } catch (ProcessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}

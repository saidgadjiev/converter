package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
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
public class Image2PdfConverter extends BaseAny2AnyConverter {

    public static final String TAG = "image2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(PNG, PHOTO), List.of(PDF),
            List.of(JPG), List.of(PDF),
            List.of(BMP), List.of(PDF),
            List.of(WEBP), List.of(PDF),
            List.of(SVG), List.of(PDF),
            List.of(HEIC, HEIF), List.of(PDF),
            List.of(ICO), List.of(PDF),
            List.of(JP2), List.of(PDF)
    );

    private ImageMagickDevice magickDevice;

    private final Image2PdfDevice image2PdfDevice;

    @Autowired
    public Image2PdfConverter(ImageMagickDevice magickDevice, Image2PdfDevice image2PdfDevice) {
        super(MAP);
        this.magickDevice = magickDevice;
        this.image2PdfDevice = image2PdfDevice;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            normalize(fileQueueItem);

            SmartTempFile src = file;
            if (fileQueueItem.getFirstFileFormat() != PNG) {
                SmartTempFile png = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, PNG.getExt());
                try {
                    magickDevice.convert2Image(file.getAbsolutePath(), png.getAbsolutePath());
                    src = png;
                } finally {
                    tempFileService().delete(file);
                }
            }
            magickDevice.changeFormatAndRemoveAlphaChannel(src.getAbsolutePath(), Format.PNG.getExt());
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, PDF.getExt());
            try {
                image2PdfDevice.convert2Pdf(src.getAbsolutePath(), result.getAbsolutePath(), FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));

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

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        tempFileService().delete(fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()));
    }

    private void normalize(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFirstFileFormat() == PHOTO) {
            fileQueueItem.getFirstFile().setFormat(JPG);
        }
    }
}

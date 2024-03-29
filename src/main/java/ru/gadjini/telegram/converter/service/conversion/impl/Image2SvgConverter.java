package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.property.Url2PdfServerProperties;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.ImageMagickDevice;
import ru.gadjini.telegram.converter.service.image.trace.ImageTracer;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Image2SvgConverter extends BaseAny2AnyConverter {

    public static final String TAG = "image2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PHOTO, Format.HEIC, Format.HEIF, Format.PNG, Format.SVG,
            Format.JP2, Format.JPG, Format.BMP, Format.WEBP), List.of(Format.SVG));

    private ImageMagickDevice imageDevice;

    private ImageTracer imageTracer;

    private Url2PdfServerProperties conversionProperties;

    @Autowired
    public Image2SvgConverter(ImageMagickDevice imageDevice, ImageTracer imageTracer, Url2PdfServerProperties conversionProperties) {
        super(MAP);
        this.imageDevice = imageDevice;
        this.imageTracer = imageTracer;
        this.conversionProperties = conversionProperties;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.SVG.getExt());
            try {
                if (fileQueueItem.getFirstFileFormat() != Format.PNG) {
                    SmartTempFile tempFile = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PNG.getExt());
                    try {
                        imageDevice.convert2Image(file.getAbsolutePath(), tempFile.getAbsolutePath(), conversionProperties.getTimeOut());
                        imageTracer.trace(tempFile.getAbsolutePath(), result.getAbsolutePath());

                        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.SVG.getExt());
                        return new FileResult(fileName, result);
                    } finally {
                        tempFileService().delete(tempFile);
                    }
                } else {
                    imageTracer.trace(file.getAbsolutePath(), result.getAbsolutePath());

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.SVG.getExt());
                    return new FileResult(fileName, result);
                }
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw e;
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}

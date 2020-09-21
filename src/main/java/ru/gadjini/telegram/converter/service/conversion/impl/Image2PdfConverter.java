package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.converter.service.image.device.Image2PdfDevice;
import ru.gadjini.telegram.converter.service.image.device.ImageMagickDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
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

    private FileManager fileManager;

    private TempFileService fileService;

    private ImageMagickDevice magickDevice;

    private final Image2PdfDevice image2PdfDevice;

    private ConversionFormatService formatService;

    @Autowired
    public Image2PdfConverter(FileManager fileManager, TempFileService fileService,
                              ImageMagickDevice magickDevice,
                              Image2PdfDevice image2PdfDevice,
                              ConversionFormatService formatService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.magickDevice = magickDevice;
        this.image2PdfDevice = image2PdfDevice;
        this.formatService = formatService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return doConvert(fileQueueItem);
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat() != PHOTO ? fileQueueItem.getFirstFileFormat().getExt() : "tmp");

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);
            normalize(file.getFile(), fileQueueItem);

            SmartTempFile src = file;
            if (fileQueueItem.getFirstFileFormat() != PNG) {
                SmartTempFile png = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, PNG.getExt());
                try {
                    magickDevice.convert2Image(file.getAbsolutePath(), png.getAbsolutePath());
                    src = png;
                } finally {
                    file.smartDelete();
                }
            }
            magickDevice.changeFormatAndRemoveAlphaChannel(src.getAbsolutePath(), Format.PNG.getExt());
            SmartTempFile tempFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, PDF.getExt());
            image2PdfDevice.convert2Pdf(src.getAbsolutePath(), tempFile.getAbsolutePath(), FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, tempFile);
        } catch (ProcessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private void normalize(File file, ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFirstFileFormat() == PHOTO) {
            Format format = formatService.getImageFormat(file, fileQueueItem.getFirstFileId());
            format = format == null ? JPG : format;
            fileQueueItem.getFirstFile().setFormat(format);
        }
    }
}

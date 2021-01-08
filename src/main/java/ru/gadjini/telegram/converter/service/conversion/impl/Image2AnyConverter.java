package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.StickerResult;
import ru.gadjini.telegram.converter.service.image.device.ImageMagickDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter {

    private static final String TAG = "image2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(PNG, PHOTO), List.of(JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(JPG), List.of(PNG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(BMP), List.of(PNG, JPG, JP2, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(WEBP), List.of(PNG, JPG, JP2, BMP, TIFF, HEIC, HEIF, STICKER),
            List.of(SVG), List.of(PNG, JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(HEIC, HEIF), List.of(JPG, JP2, BMP, WEBP, TIFF, STICKER),
            List.of(ICO), List.of(PNG, JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, STICKER),
            List.of(JP2), List.of(PNG, JPG, BMP, WEBP, TIFF, HEIC, HEIF, STICKER)
    );

    private ImageMagickDevice imageDevice;

    @Autowired
    public Image2AnyConverter(ImageMagickDevice imageDevice) {
        super(MAP);
        this.imageDevice = imageDevice;
    }

    @Override
    public FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            normalize(fileQueueItem);

            SmartTempFile tempFile = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                imageDevice.convert2Image(file.getAbsolutePath(), tempFile.getAbsolutePath(), getOptions(fileQueueItem.getTargetFormat()));

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return fileQueueItem.getTargetFormat() == STICKER
                        ? new StickerResult(tempFile)
                        : new FileResult(fileName, tempFile);
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

    private void normalize(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFirstFileFormat() == PHOTO) {
            fileQueueItem.getFirstFile().setFormat(JPG);
        }
    }

    private String[] getOptions(Format targetFormat) {
        if (targetFormat == JPG) {
            return new String[]{
                    "-alpha", "remove"
            };
        }

        return new String[0];
    }
}

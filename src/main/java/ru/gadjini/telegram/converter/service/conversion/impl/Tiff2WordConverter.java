package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.tiff.TiffFrame;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.words.DocumentBuilder;
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
public class Tiff2WordConverter extends BaseAny2AnyConverter {

    public static final String TAG = "tiff2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TIFF), List.of(Format.DOC, Format.DOCX)
    );

    @Autowired
    public Tiff2WordConverter() {
        super(MAP);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        return toWord(fileQueueItem);
    }

    private FileResult toWord(ConversionQueueItem fileQueueItem) {
        SmartTempFile tiff = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try (TiffImage image = (TiffImage) Image.load(tiff.getAbsolutePath())) {
            DocumentBuilder documentBuilder = new DocumentBuilder();
            try {
                for (TiffFrame tiffFrame : image.getFrames()) {
                    documentBuilder.insertImage(tiffFrame.toBitmap());
                }
                SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    documentBuilder.getDocument().save(result.getAbsolutePath());

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw e;
                }
            } finally {
                documentBuilder.getDocument().cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }
}

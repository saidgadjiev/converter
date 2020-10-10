package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.tiff.TiffFrame;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.words.DocumentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Tiff2WordConverter extends BaseAny2AnyConverter {

    public static final String TAG = "tiff2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TIFF), List.of(Format.DOC, Format.DOCX)
    );

    private FileManager fileManager;

    private TempFileService fileService;

    @Autowired
    public Tiff2WordConverter(FileManager fileManager, TempFileService fileService) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return toWord(fileQueueItem);
    }

    private FileResult toWord(ConversionQueueItem fileQueueItem) {
        SmartTempFile tiff = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, tiff);
            try (TiffImage image = (TiffImage) Image.load(tiff.getAbsolutePath())) {
                DocumentBuilder documentBuilder = new DocumentBuilder();
                try {
                    for (TiffFrame tiffFrame : image.getFrames()) {
                        documentBuilder.insertImage(tiffFrame.toBitmap());
                    }
                    SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
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
        } finally {
            tiff.smartDelete();
        }
    }
}

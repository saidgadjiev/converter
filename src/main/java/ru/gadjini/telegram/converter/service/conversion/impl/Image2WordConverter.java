package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
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
public class Image2WordConverter extends BaseAny2AnyConverter {

    public static final String TAG = "image2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.PHOTO, Format.HEIC, Format.HEIF, Format.PNG, Format.SVG, Format.JP2, Format.JPG, Format.BMP, Format.WEBP),
            List.of(Format.DOC, Format.DOCX)
    );

    @Autowired
    public Image2WordConverter() {
        super(MAP);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            normalize(fileQueueItem);

            Document document = new Document();
            try {
                DocumentBuilder documentBuilder = new DocumentBuilder(document);
                documentBuilder.insertImage(file.getAbsolutePath());
                SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    documentBuilder.getDocument().save(out.getAbsolutePath());

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, out);
                } catch (Throwable e) {
                    out.smartDelete();
                    throw e;
                }
            } finally {
                document.cleanup();
            }
        } catch (Exception e) {
            throw new ConvertException(e);
        }
    }

    private void normalize(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFirstFileFormat() == Format.PHOTO) {
            fileQueueItem.getFirstFile().setFormat(Format.JPG);
        }
    }
}

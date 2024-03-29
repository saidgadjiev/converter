package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.devices.TiffDevice;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Word2TiffConverter extends BaseAny2AnyConverter {

    public static final String TAG = "word2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.DOC, Format.DOCX), List.of(Format.TIFF)
    );

    @Autowired
    public Word2TiffConverter() {
        super(MAP);
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem queueItem) {
        return toTiff(queueItem);
    }

    private FileResult toTiff(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            Document word = new Document(file.getAbsolutePath());
            SmartTempFile pdfFile = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), "any2any", Format.PDF.getExt());
            try {
                word.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);
            } finally {
                word.cleanup();
            }
            com.aspose.pdf.Document pdf = new com.aspose.pdf.Document(pdfFile.getAbsolutePath());
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, Format.TIFF.getExt());
            try {
                try {
                    pdf.optimizeResources();
                    TiffDevice tiffDevice = new TiffDevice();
                    tiffDevice.process(pdf, result.getAbsolutePath());
                } finally {
                    pdf.dispose();
                    tempFileService().delete(pdfFile);
                }

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.TIFF.getExt());
                return new FileResult(fileName, result);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw e;
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}

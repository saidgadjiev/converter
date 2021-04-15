package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.devices.TiffDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Pdf2TiffConverter extends BaseAny2AnyConverter {

    public static final String TAG = "pdf2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.PDF), List.of(Format.TIFF));

    @Autowired
    public Pdf2TiffConverter() {
        super(MAP);
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        return toTiff(fileQueueItem);
    }

    private FileResult toTiff(ConversionQueueItem fileQueueItem) {
        SmartTempFile pdfFile = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        Document pdf = new Document(pdfFile.getAbsolutePath());
        try {
            pdf.optimize();
            pdf.optimizeResources();
            TiffDevice tiffDevice = new TiffDevice();
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.TIFF.getExt());
            try {
                tiffDevice.process(pdf, result.getAbsolutePath());

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.TIFF.getExt());
                return new FileResult(fileName, result);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw e;
            }
        } finally {
            pdf.dispose();
        }
    }
}

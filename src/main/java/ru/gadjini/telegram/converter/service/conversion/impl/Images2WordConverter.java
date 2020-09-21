package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Images2WordConverter extends BaseAny2AnyConverter {

    private static final String TAG = "images2word";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGES), List.of(Format.DOC, Format.DOCX)
    );

    private TempFileService fileService;

    private Images2PdfTiffConverter images2PdfTiffConverter;

    @Autowired
    public Images2WordConverter(TempFileService fileService, Images2PdfTiffConverter images2PdfTiffConverter) {
        super(MAP);
        this.fileService = fileService;
        this.images2PdfTiffConverter = images2PdfTiffConverter;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        Format originalFormat = fileQueueItem.getTargetFormat();
        fileQueueItem.setTargetFormat(Format.PDF);
        try (FileResult fileResult = (FileResult) images2PdfTiffConverter.convert(fileQueueItem)) {
            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), TAG, originalFormat.getExt());
            Document document = new Document(fileResult.getFile().getAbsolutePath());
            try {
                document.save(result.getAbsolutePath(), originalFormat == Format.DOC ? SaveFormat.Doc : SaveFormat.DocX);
            } finally {
                document.dispose();
            }

            String fileName = Any2AnyFileNameUtils.getFileName(fileResult.getFileName(), originalFormat.getExt());
            return new FileResult(fileName, result);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}

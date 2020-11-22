package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.device.SmartCalibre;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Epub2WordConverter extends BaseAny2AnyConverter {

    private static final String TAG = "epub2word";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.EPUB), List.of(Format.DOCX, Format.DOC)
    );

    private SmartCalibre convertDevice;

    private Pdf2WordConverter pdf2WordConverter;

    @Autowired
    public Epub2WordConverter(SmartCalibre convertDevice, Pdf2WordConverter pdf2WordConverter) {
        super(MAP);
        this.convertDevice = convertDevice;
        this.pdf2WordConverter = pdf2WordConverter;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile epub = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            SmartTempFile pdf = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
            try {
                convertDevice.convert(epub.getAbsolutePath(), pdf.getAbsolutePath());

                return pdf2WordConverter.convert(fileQueueItem, pdf);
            } finally {
                pdf.smartDelete();
            }
        } finally {
            epub.smartDelete();
        }
    }
}

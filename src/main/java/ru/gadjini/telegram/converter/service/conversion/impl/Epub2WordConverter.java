package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.device.ConvertDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Epub2WordConverter extends BaseAny2AnyConverter {

    private static final String TAG = "epub2word";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.EPUB), List.of(Format.DOCX, Format.DOC)
    );

    private ConvertDevice convertDevice;

    private FileManager fileManager;

    private TempFileService tempFileService;

    private Pdf2WordConverter pdf2WordConverter;

    @Autowired
    public Epub2WordConverter(ConvertDevice convertDevice, FileManager fileManager, TempFileService tempFileService, Pdf2WordConverter pdf2WordConverter) {
        super(MAP);
        this.convertDevice = convertDevice;
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
        this.pdf2WordConverter = pdf2WordConverter;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile epub = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, epub);

            SmartTempFile pdf = tempFileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.PDF.getExt());
            try {
                convertDevice.convert(epub.getAbsolutePath(), pdf.getAbsolutePath(), FilenameUtils.removeExtension(fileQueueItem.getFirstFileName()));

                return pdf2WordConverter.convert(fileQueueItem, pdf);
            } finally {
                pdf.smartDelete();
            }
        } finally {
            epub.smartDelete();
        }
    }
}

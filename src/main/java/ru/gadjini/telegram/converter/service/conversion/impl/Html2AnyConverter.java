package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.html.HtmlDevice;
import ru.gadjini.telegram.converter.service.html.Url2PdfApiDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Html2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "html2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.HTML), List.of(Format.PDF));

    private FileManager fileManager;

    private TempFileService fileService;

    private HtmlDevice htmlDevice;

    @Autowired
    public Html2AnyConverter(FileManager fileManager, TempFileService fileService, @Qualifier("api") HtmlDevice htmlDevice) {
        super(MAP);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.htmlDevice = htmlDevice;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        return htmlToPdf(fileQueueItem);
    }

    private FileResult htmlToPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile html = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.forceDownloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, html);
            SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                htmlDevice.convertHtml(html.getAbsolutePath(), file.getAbsolutePath(), getOutputType(fileQueueItem.getTargetFormat()));

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, file);
            } catch (Throwable e) {
                file.smartDelete();
                throw e;
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            html.smartDelete();
        }
    }

    private String getOutputType(Format target) {
        switch (target) {
            case PDF:
                return Url2PdfApiDevice.PDF_OUTPUT;
            case PNG:
                return Url2PdfApiDevice.SCREENSHOT_OUTPUT;
            default:
                return Url2PdfApiDevice.HTML_OUTPUT;
        }
    }
}

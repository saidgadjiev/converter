package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.converter.service.html.HtmlDevice;
import ru.gadjini.telegram.converter.service.html.Url2PdfApiDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Html2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    public static final String TAG = "html2";

    private FileManager fileManager;

    private TempFileService fileService;

    private HtmlDevice htmlDevice;

    @Autowired
    public Html2AnyConverter(ConversionFormatService formatService, FileManager fileManager,
                             TempFileService fileService, @Qualifier("api") HtmlDevice htmlDevice) {
        super(Set.of(Format.HTML), formatService);
        this.fileManager = fileManager;
        this.fileService = fileService;
        this.htmlDevice = htmlDevice;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        return htmlToPdf(fileQueueItem);
    }

    private FileResult htmlToPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile html = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getSize(), html);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            htmlDevice.convertHtml(html.getAbsolutePath(), file.getAbsolutePath(), getOutputType(fileQueueItem.getTargetFormat()));

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, file, stopWatch.getTime(TimeUnit.SECONDS));
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

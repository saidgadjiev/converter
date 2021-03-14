package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.html.HtmlDevice;
import ru.gadjini.telegram.converter.service.html.Url2PdfApiDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Html2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "html2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(List.of(Format.HTML), List.of(Format.PDF, Format.PNG));

    private HtmlDevice htmlDevice;

    @Autowired
    public Html2AnyConverter(@Qualifier("api") HtmlDevice htmlDevice) {
        super(MAP);
        this.htmlDevice = htmlDevice;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        return htmlToPdf(fileQueueItem);
    }

    private FileResult htmlToPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile html = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                htmlDevice.convertHtml(html.getAbsolutePath(), result.getAbsolutePath(), getOutputType(fileQueueItem.getTargetFormat()));

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, result);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw e;
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
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

package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.DocumentBuilder;
import com.aspose.words.Font;
import com.aspose.words.SaveFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.io.SmartTempFile;
import ru.gadjini.telegram.converter.service.TempFileService;
import ru.gadjini.telegram.converter.service.conversion.api.Format;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.text.TextDetector;
import ru.gadjini.telegram.converter.service.text.TextDirection;
import ru.gadjini.telegram.converter.service.text.TextInfo;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.TextUtils;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Text2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "text2";

    private static final Logger LOGGER = LoggerFactory.getLogger(Text2AnyConverter.class);

    private TempFileService fileService;

    private TextDetector textDetector;

    @Autowired
    public Text2AnyConverter(FormatService formatService, TempFileService fileService, TextDetector textDetector) {
        super(Set.of(Format.TEXT), formatService);
        this.fileService = fileService;
        this.textDetector = textDetector;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.TXT) {
            return toTxt(fileQueueItem);
        }
        return toWordOrPdf(fileQueueItem);
    }

    private FileResult toTxt(ConversionQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), TAG, Format.TXT.getExt());
            TextInfo textInfo = textDetector.detect(fileQueueItem.getFileId());
            LOGGER.debug("Text info({})", textInfo);
            String text = TextUtils.removeAllEmojis(fileQueueItem.getFileId(), textInfo.getDirection());
            FileUtils.writeStringToFile(result.getFile(), text, StandardCharsets.UTF_8);

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.TXT.getExt());
            return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    private FileResult toWordOrPdf(ConversionQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            com.aspose.words.Document document = new com.aspose.words.Document();
            try {
                DocumentBuilder documentBuilder = new DocumentBuilder(document);
                Font font = documentBuilder.getFont();
                font.setColor(Color.BLACK);

                TextInfo textInfo = textDetector.detect(fileQueueItem.getFileId());
                LOGGER.debug("Text info({})", textInfo);
                String text = TextUtils.removeAllEmojis(fileQueueItem.getFileId(), textInfo.getDirection());
                if (textInfo.getDirection() == TextDirection.LR) {
                    font.setSize(textInfo.getFont().getPrimarySize());
                    font.setName(textInfo.getFont().getFontName());
                } else {
                    font.setSizeBi(textInfo.getFont().getPrimarySize());
                    font.setNameBi(textInfo.getFont().getFontName());
                    font.setBidi(true);
                    documentBuilder.getParagraphFormat().setBidi(true);
                }

                documentBuilder.write(text);
                SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), TAG, fileQueueItem.getTargetFormat().getExt());
                document.save(result.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case PDF:
                return SaveFormat.PDF;
            case DOC:
                return SaveFormat.DOC;
            default:
                return SaveFormat.DOCX;
        }
    }
}

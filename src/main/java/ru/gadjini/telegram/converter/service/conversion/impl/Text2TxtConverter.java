package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.text.TextDetector;
import ru.gadjini.telegram.converter.service.text.TextInfo;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.TextUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class Text2TxtConverter extends BaseAny2AnyConverter {

    private static final String TAG = "text2";

    private static final Logger LOGGER = LoggerFactory.getLogger(Text2TxtConverter.class);

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TEXT), List.of(Format.TXT)
    );

    private TempFileService fileService;

    private TextDetector textDetector;

    @Autowired
    public Text2TxtConverter(TempFileService fileService, TextDetector textDetector) {
        super(MAP);
        this.fileService = fileService;
        this.textDetector = textDetector;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return toTxt(fileQueueItem);
    }

    private FileResult toTxt(ConversionQueueItem fileQueueItem) {
        SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), TAG, Format.TXT.getExt());
        try {
            TextInfo textInfo = textDetector.detect(fileQueueItem.getFirstFileId());
            LOGGER.debug("Text info({})", textInfo);
            String text = TextUtils.removeAllEmojis(fileQueueItem.getFirstFileId(), textInfo.getDirection());
            FileUtils.writeStringToFile(result.getFile(), text, StandardCharsets.UTF_8);

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.TXT.getExt());
            return new FileResult(fileName, result, null);
        } catch (Exception ex) {
            result.smartDelete();
            throw new ConvertException(ex);
        }
    }
}

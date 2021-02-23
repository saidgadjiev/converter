package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.text.TextDetector;
import ru.gadjini.telegram.converter.service.text.TextInfo;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.TextUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class Text2FileConverter extends BaseAny2AnyConverter {

    private static final String TAG = "text2";

    private static final Logger LOGGER = LoggerFactory.getLogger(Text2FileConverter.class);

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TEXT), List.of(Format.TXT, Format.CSV, Format.XML)
    );

    private TextDetector textDetector;

    @Autowired
    public Text2FileConverter(TextDetector textDetector) {
        super(MAP);
        this.textDetector = textDetector;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        return writeText2File(fileQueueItem);
    }

    private FileResult writeText2File(ConversionQueueItem fileQueueItem) {
        SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), TAG, fileQueueItem.getTargetFormat().getExt());
        try {
            TextInfo textInfo = textDetector.detect(fileQueueItem.getFirstFileId());
            LOGGER.debug("Text info({})", textInfo);
            String text = TextUtils.removeAllEmojis(fileQueueItem.getFirstFileId(), textInfo.getDirection());
            FileUtils.writeStringToFile(result.getFile(), text, StandardCharsets.UTF_8);

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, result);
        } catch (Exception ex) {
            result.smartDelete();
            throw new ConvertException(ex);
        }
    }
}

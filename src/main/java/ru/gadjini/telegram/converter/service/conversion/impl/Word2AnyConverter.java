package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.FormatMapUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class Word2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "word2any";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>();

    static {
        MAP.put(List.of(Format.DOC), List.of(Format.DOT, Format.DOCX, Format.DOCM, Format.DOTX, Format.DOTM, Format.XPS, Format.SVG, Format.PS, Format.PCL, Format.HTML, Format.MHTML, Format.ODT, Format.OTT, Format.TXT));
        MAP.put(List.of(Format.DOT), List.of(Format.DOC, Format.DOCX, Format.DOCM, Format.DOTX, Format.DOTM, Format.XPS, Format.SVG, Format.PS, Format.PCL, Format.HTML, Format.MHTML, Format.ODT, Format.OTT, Format.TXT));
        MAP.put(List.of(Format.DOCX), List.of(Format.DOT, Format.DOC, Format.DOCM, Format.DOTX, Format.DOTM, Format.XPS, Format.SVG, Format.PS, Format.PCL, Format.HTML, Format.MHTML, Format.ODT, Format.OTT, Format.TXT));

        Set<Format> loadFormats = Set.of(Format.DOT, Format.DOCM, Format.DOTX, Format.DOTM, Format.MHTML, Format.ODT, Format.OTT, Format.TXT);
        Set<Format> saveFormats = Set.of(Format.DOC, Format.DOT, Format.DOCX, Format.DOCM, Format.DOTX, Format.DOTM, Format.XPS, Format.SVG, Format.PS, Format.PCL,
                Format.HTML, Format.MHTML, Format.ODT, Format.OTT, Format.TXT);

        MAP.putAll(FormatMapUtils.buildMap(loadFormats, saveFormats));
    }

    @Autowired
    public Word2AnyConverter() {
        super(MAP);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());
        try {
            Document document = new Document(file.getAbsolutePath());
            try {
                SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    document.save(result.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, result, null);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw e;
                }
            } finally {
                document.cleanup();
            }
        } catch (Throwable ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case DOC:
                return SaveFormat.DOC;
            case DOCX:
                return SaveFormat.DOCX;
            case DOCM:
                return SaveFormat.DOCM;
            case DOTX:
                return SaveFormat.DOTX;
            case DOTM:
                return SaveFormat.DOTM;
            case XPS:
                return SaveFormat.XPS;
            case SVG:
                return SaveFormat.SVG;
            case PS:
                return SaveFormat.PS;
            case PCL:
                return SaveFormat.PCL;
            case HTML:
                return SaveFormat.HTML;
            case MHTML:
                return SaveFormat.MHTML;
            case ODT:
                return SaveFormat.ODT;
            case OTT:
                return SaveFormat.OTT;
            case TXT:
                return SaveFormat.TEXT;
        }

        throw new IllegalArgumentException("Not found format to save " + format);
    }
}

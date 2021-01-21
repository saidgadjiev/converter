package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.pdf.*;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Pdf2AnyConverter extends BaseAny2AnyConverter {

    private static final String TAG = "pdf2xml";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.PDF), List.of(Format.HTML, Format.PPTX, Format.SVG, Format.XML, Format.XPS),
            List.of(Format.MHT), List.of(Format.HTML, Format.PPTX, Format.SVG, Format.XML, Format.XPS, Format.DOC, Format.DOCX, Format.PDF),
            List.of(Format.CGM), List.of(Format.HTML, Format.PPTX, Format.SVG, Format.XML, Format.XPS, Format.DOC, Format.DOCX, Format.PDF),
            List.of(Format.PCL), List.of(Format.HTML, Format.PPTX, Format.SVG, Format.XML, Format.XPS, Format.DOC, Format.DOCX, Format.PDF),
            List.of(Format.PS), List.of(Format.HTML, Format.PPTX, Format.SVG, Format.XML, Format.XPS, Format.DOC, Format.DOCX, Format.PDF),
            List.of(Format.XPS), List.of(Format.HTML, Format.PPTX, Format.SVG, Format.XML, Format.XPS, Format.DOC, Format.DOCX, Format.PDF),
            List.of(Format.SVG), List.of(Format.HTML, Format.PPTX, Format.XML, Format.XPS)
    );

    public Pdf2AnyConverter() {
        super(MAP);
    }

    @Override
    protected ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            Document document;
            LoadOptions loadOptions = getLoadOptions(fileQueueItem.getFirstFileFormat());
            if (loadOptions != null) {
                document = new Document(file.getAbsolutePath(), loadOptions);
            } else {
                document = new Document(file.getAbsolutePath());
            }
            try {
                SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    document.save(result.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, result);
                } catch (Throwable e) {
                    result.smartDelete();
                    throw e;
                }
            } finally {
                document.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    private LoadOptions getLoadOptions(Format format) {
        switch (format) {
            case PDF:
                return null;
            case MHT:
                return new MhtLoadOptions();
            case CGM:
                return new CgmLoadOptions();
            case PCL:
                return new PclLoadOptions();
            case PS:
                return new PsLoadOptions();
            case XPS:
                return new XpsLoadOptions();
            case SVG:
                return new SvgLoadOptions();
        }

        throw new IllegalArgumentException("Unsupported format to load " + format);
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case HTML:
                return SaveFormat.Html;
            case PPTX:
                return SaveFormat.Pptx;
            case SVG:
                return SaveFormat.Svg;
            case XML:
                return SaveFormat.Xml;
            case XPS:
                return SaveFormat.Xps;
            case DOC:
                return SaveFormat.Doc;
            case DOCX:
                return SaveFormat.DocX;
            case PDF:
                return SaveFormat.Pdf;
        }

        throw new IllegalArgumentException("Unsupported format to save " + format);
    }
}

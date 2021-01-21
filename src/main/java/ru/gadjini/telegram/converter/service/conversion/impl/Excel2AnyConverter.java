package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
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
public class Excel2AnyConverter extends BaseAny2AnyConverter {

    public static final String TAG = "excel2";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>();

    static {
        MAP.put(List.of(Format.MHTML), List.of(Format.CSV, Format.XLSX, Format.XLSM, Format.XLTX, Format.XLTM,
                Format.XLAM, Format.TSV, Format.ODS, Format.XLSB,
                Format.DIF, Format.NUMBERS, Format.FODS, Format.SXC, Format.XLS));
        MAP.put(List.of(Format.HTML), List.of(Format.CSV, Format.XLSX, Format.XLSM, Format.XLTX, Format.XLTM,
                Format.XLAM, Format.TSV, Format.ODS, Format.XLSB, Format.XPS,
                Format.DIF, Format.NUMBERS, Format.FODS, Format.SXC, Format.XLS));

        Set<Format> asposeCellsSaveFormats = Set.of(Format.CSV, Format.XLSX, Format.XLSM, Format.XLTX, Format.XLTM,
                Format.XLAM, Format.TSV, Format.HTML, Format.MHTML, Format.ODS, Format.XLSB, Format.PDF, Format.XPS,
                Format.TIFF, Format.SVG, Format.DIF, Format.NUMBERS, Format.FODS, Format.SXC, Format.XLS);

        Set<Format> asposeCellsLoadFormats = Set.of(
                Format.CSV, Format.XLSX, Format.XLS, Format.TSV, Format.ODS,
                Format.XLSB, Format.NUMBERS, Format.FODS, Format.SXC
        );

        MAP.putAll(FormatMapUtils.buildMap(asposeCellsLoadFormats, asposeCellsSaveFormats));
    }

    @Autowired
    public Excel2AnyConverter() {
        super(MAP);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());
        try {
            Workbook workbook = new Workbook(file.getAbsolutePath());
            try {
                SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
                try {
                    workbook.save(out.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, out);
                } catch (Throwable e) {
                    out.smartDelete();
                    throw e;
                }
            } finally {
                workbook.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case PDF:
                return SaveFormat.PDF;
            case CSV:
                return SaveFormat.CSV;
            case XLSX:
                return SaveFormat.XLSX;
            case XLS:
                return 5;
            case XLSM:
                return SaveFormat.XLSM;
            case XLTX:
                return SaveFormat.XLTX;
            case XLTM:
                return SaveFormat.XLTM;
            case XLAM:
                return SaveFormat.XLAM;
            case TSV:
                return SaveFormat.TSV;
            case MHTML:
                return SaveFormat.M_HTML;
            case ODS:
                return SaveFormat.ODS;
            case XLSB:
                return SaveFormat.XLSB;
            case XPS:
                return SaveFormat.XPS;
            case TIFF:
                return SaveFormat.TIFF;
            case SVG:
                return SaveFormat.SVG;
            case DIF:
                return SaveFormat.DIF;
            case NUMBERS:
                return SaveFormat.NUMBERS;
            case FODS:
                return SaveFormat.FODS;
            case SXC:
                return SaveFormat.SXC;
        }

        throw new IllegalArgumentException("Save format not found for " + format);
    }
}

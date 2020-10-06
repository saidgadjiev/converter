package ru.gadjini.telegram.converter.service.conversion.format;

import com.aspose.imaging.FileFormat;
import com.aspose.imaging.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class ConversionFormatService {

    public static final String TAG = "format";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionFormatService.class);

    private Map<FormatCategory, Map<List<Format>, List<Format>>> formats;

    @Autowired
    public ConversionFormatService(Map<FormatCategory, Map<List<Format>, List<Format>>> formats) {
        this.formats = formats;
    }

    public List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<FormatCategory, Map<List<Format>, List<Format>>> categoryEntry : formats.entrySet()) {
            for (Map.Entry<List<Format>, List<Format>> entry : categoryEntry.getValue().entrySet()) {
                if (entry.getKey().contains(srcFormat)) {
                    return entry.getValue();
                }
            }
        }

        return Collections.emptyList();
    }

    public boolean isSupportedCategory(FormatCategory category) {
        return formats.containsKey(category);
    }

    public boolean isConvertAvailable(Format src, Format target) {
        return getTargetFormats(src).contains(target);
    }

    public Format getImageFormat(File file, String photoFileId) {
        try {
            long format = Image.getFileFormat(file.getAbsolutePath());

            return getImageFormat(format);
        } catch (Exception ex) {
            LOGGER.error("Error format detect " + photoFileId + "\n" + ex.getMessage(), ex);
            return null;
        }
    }

    private Format getImageFormat(long format) {
        if (format == FileFormat.Bmp) {
            return BMP;
        } else if (format == FileFormat.Png) {
            return PNG;
        } else if (format == FileFormat.Jpeg) {
            return JPG;
        } else if (format == FileFormat.Tiff) {
            return TIFF;
        } else if (format == FileFormat.Webp) {
            return WEBP;
        } else if (format == FileFormat.Svg) {
            return SVG;
        } else {
            return null;
        }
    }
}

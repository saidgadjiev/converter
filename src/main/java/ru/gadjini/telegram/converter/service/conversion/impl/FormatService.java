package ru.gadjini.telegram.converter.service.conversion.impl;

import com.aspose.imaging.FileFormat;
import com.aspose.imaging.Image;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.conversion.api.Format;
import ru.gadjini.telegram.converter.service.conversion.api.FormatCategory;
import ru.gadjini.telegram.converter.utils.MimeTypeUtils;
import ru.gadjini.telegram.converter.utils.UrlUtils;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FormatService {

    public static final String TAG = "format";

    private static final Map<FormatCategory, Map<List<Format>, List<Format>>> FORMATS = new LinkedHashMap<>();

    static {
        Map<List<Format>, List<Format>> documents = new LinkedHashMap<>();
        documents.put(List.of(Format.DOC), List.of(Format.PDF, Format.DOCX, Format.TXT, Format.EPUB, Format.RTF, Format.TIFF));
        documents.put(List.of(Format.DOCX), List.of(Format.PDF, Format.DOC, Format.TXT, Format.EPUB, Format.RTF, Format.TIFF));
        documents.put(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX, Format.EPUB, Format.TIFF));
        documents.put(List.of(Format.TEXT), List.of(Format.PDF, Format.DOC, Format.DOCX, Format.TXT));
        documents.put(List.of(Format.TXT), List.of(Format.PDF, Format.DOC, Format.DOCX));
        documents.put(List.of(Format.EPUB), List.of(Format.PDF, Format.DOC, Format.DOCX, Format.RTF));
        documents.put(List.of(Format.URL, Format.HTML), List.of(Format.PDF, Format.PNG));
        documents.put(List.of(Format.XLS, Format.XLSX), List.of(Format.PDF));
        documents.put(List.of(Format.PPTX, Format.PPT, Format.PPTM, Format.POTX, Format.POT, Format.POTM, Format.PPS, Format.PPSX, Format.PPSM), List.of(Format.PDF));
        FORMATS.put(FormatCategory.DOCUMENTS, documents);

        Map<List<Format>, List<Format>> images = new LinkedHashMap<>();
        images.put(List.of(Format.PNG), List.of(Format.PDF, Format.DOC, Format.DOCX, Format.JPG, Format.JP2, Format.BMP, Format.WEBP, Format.TIFF, Format.ICO, Format.HEIC, Format.HEIF, Format.SVG, Format.STICKER));
        images.put(List.of(Format.PHOTO), List.of(Format.PDF, Format.DOC, Format.DOCX, Format.PNG, Format.JPG, Format.JP2, Format.BMP, Format.WEBP, Format.TIFF, Format.ICO, Format.HEIC, Format.HEIF, Format.SVG, Format.STICKER));
        images.put(List.of(Format.JPG), List.of(Format.PDF, Format.DOC, Format.DOCX, Format.PNG, Format.JP2, Format.BMP, Format.WEBP, Format.TIFF, Format.ICO, Format.HEIC, Format.HEIF, Format.SVG, Format.STICKER));
        images.put(List.of(Format.TIFF), List.of(Format.PDF, Format.DOCX, Format.DOC));
        images.put(List.of(Format.BMP), List.of(Format.PDF, Format.PNG, Format.JPG, Format.JP2, Format.WEBP, Format.TIFF, Format.ICO, Format.HEIC, Format.HEIF, Format.SVG, Format.STICKER));
        images.put(List.of(Format.WEBP), List.of(Format.PDF, Format.PNG, Format.JPG, Format.JP2, Format.BMP, Format.TIFF, Format.ICO, Format.HEIC, Format.HEIF, Format.SVG, Format.STICKER));
        images.put(List.of(Format.SVG), List.of(Format.PDF, Format.PNG, Format.JPG, Format.JP2, Format.BMP, Format.WEBP, Format.TIFF, Format.ICO, Format.HEIC, Format.HEIF, Format.STICKER));
        images.put(List.of(Format.HEIC, Format.HEIF), List.of(Format.PDF, Format.PNG, Format.JPG, Format.JP2, Format.BMP, Format.WEBP, Format.TIFF, Format.ICO, Format.SVG, Format.STICKER));
        images.put(List.of(Format.ICO), List.of(Format.PDF, Format.PNG, Format.JPG, Format.JP2, Format.BMP, Format.WEBP, Format.TIFF, Format.HEIC, Format.HEIF, Format.SVG, Format.STICKER));
        images.put(List.of(Format.JP2), List.of(Format.PDF, Format.PNG, Format.JPG, Format.BMP, Format.WEBP, Format.TIFF, Format.ICO, Format.HEIC, Format.HEIF, Format.SVG, Format.STICKER));
        images.put(List.of(Format.TGS), List.of(Format.GIF));
        FORMATS.put(FormatCategory.IMAGES, images);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FormatService.class);

    public List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<FormatCategory, Map<List<Format>, List<Format>>> categoryEntry : FORMATS.entrySet()) {
            for (Map.Entry<List<Format>, List<Format>> entry : categoryEntry.getValue().entrySet()) {
                if (entry.getKey().contains(srcFormat)) {
                    return entry.getValue();
                }
            }
        }

        return Collections.emptyList();
    }

    public Format getAssociatedFormat(String format) {
        if ("jpeg".equals(format)) {
            return Format.JPG;
        }
        format = format.toUpperCase();
        for (Format f : Format.values()) {
            if (f.name().equals(format)) {
                return f;
            }
        }

        return null;
    }

    public Format getFormat(String text) {
        if (UrlUtils.isUrl(text)) {
            return Format.URL;
        }

        return Format.TEXT;
    }

    public String getExt(String mimeType) {
        return getExt(null, mimeType);
    }

    public String getExt(String fileName, String mimeType) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension) && !".bin".equals(extension)) {
            extension = extension.substring(1);
            if (extension.equals("mpga")) {
                return "mp3";
            }
        } else {
            extension = FilenameUtils.getExtension(fileName);
        }

        if ("jpeg".equals(extension)) {
            return "jpg";
        }

        return StringUtils.isBlank(extension) ? "bin" : extension;
    }

    public Format getFormat(String fileName, String mimeType) {
        String extension = getExt(fileName, mimeType);
        if (StringUtils.isBlank(extension)) {
            return null;
        }

        for (Format format : Format.values()) {
            if (format.getExt().equals(extension)) {
                return format;
            }
        }

        return null;
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

    public boolean isConvertAvailable(Format src, Format target) {
        return getTargetFormats(src).contains(target);
    }

    private Format getImageFormat(long format) {
        if (format == FileFormat.Bmp) {
            return Format.BMP;
        } else if (format == FileFormat.Png) {
            return Format.PNG;
        } else if (format == FileFormat.Jpeg) {
            return Format.JPG;
        } else if (format == FileFormat.Tiff) {
            return Format.TIFF;
        } else if (format == FileFormat.Webp) {
            return Format.WEBP;
        } else if (format == FileFormat.Svg) {
            return Format.SVG;
        } else {
            return null;
        }
    }
}

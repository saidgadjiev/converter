package ru.gadjini.telegram.converter.utils;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

public class MimeTypeUtils {

    private MimeTypeUtils() {}

    public static String getExtension(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
        MimeType parsedMimeType;
        try {
            parsedMimeType = allTypes.forName(mimeType);
        } catch (MimeTypeException e) {
            return null;
        }

        return parsedMimeType.getExtension();
    }
}

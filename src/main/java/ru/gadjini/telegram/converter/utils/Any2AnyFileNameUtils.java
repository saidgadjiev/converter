package ru.gadjini.telegram.converter.utils;

import org.apache.commons.io.FilenameUtils;

public class Any2AnyFileNameUtils {

    private Any2AnyFileNameUtils() {}

    public static String getFileName(String fileName, String ext) {
        return FilenameUtils.removeExtension(fileName) + "." + ext;
    }
}

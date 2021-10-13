package ru.gadjini.telegram.converter.utils;

public class BitrateUtils {

    private BitrateUtils() {

    }

    public static int toKBytes(int bytes) {
        return bytes / 1024;
    }
}

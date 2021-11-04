package ru.gadjini.telegram.converter.utils;

public class BitrateUtils {

    private BitrateUtils() {

    }

    public static int toBytes(int kBytes) {
        return kBytes * 1024;
    }

    public static int toKBytes(int bytes) {
        return bytes / 1024;
    }
}

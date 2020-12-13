package ru.gadjini.telegram.converter.utils;

import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.*;

public class FormatMapUtils {

    private FormatMapUtils() {

    }

    public static Map<List<Format>, List<Format>> buildMap(Set<Format> loadFormats, Set<Format> saveFormats) {
        Map<List<Format>, List<Format>> map = new HashMap<>();

        for (Format loadFormat : loadFormats) {
            List<Format> sf = new ArrayList<>(saveFormats);
            sf.remove(loadFormat);
            map.put(List.of(loadFormat), sf);
        }

        return map;
    }

    public static void print(Map<List<Format>, List<Format>> formatMap) {
        formatMap.forEach((formats, formats2) -> {
            System.out.print("documents.put(List.of(");
            for (Format format : formats) {
                System.out.print(format + ", ");
            }
            System.out.print("), List.of(");
            for (Format format : formats2) {
                System.out.print(format + ", ");
            }
            System.out.print("));");
            System.out.println();
        });
    }
}

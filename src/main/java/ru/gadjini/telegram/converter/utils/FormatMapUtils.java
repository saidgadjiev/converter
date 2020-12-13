package ru.gadjini.telegram.converter.utils;

import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

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

    public static void validateAndPrint(Set<Any2AnyConverter> converters) {
        System.out.println("Start checking");
        Map<String, Class<?>> keys = new HashMap<>();
        Map<Format, List<Format>> result = new HashMap<>();
        for (Any2AnyConverter any2AnyConverter : converters) {
            any2AnyConverter.getConversionMap().forEach((formats, formats2) -> {
                for (Format format : formats) {
                    for (Format format1 : formats2) {
                        String key = format.name() + "->" + format1.name();
                        if (keys.containsKey(key)) {
                            throw new RuntimeException(key + ":" + any2AnyConverter.getClass() + ":" + keys.get(key));
                        }

                        keys.put(key, any2AnyConverter.getClass());
                        result.putIfAbsent(format, new ArrayList<>());
                        result.get(format).add(format1);
                    }
                }
            });
        }

        Map<Format, List<Format>> r = new LinkedHashMap<>();
        List<FormatCategory> formatCategories = Arrays.asList(FormatCategory.values());
        formatCategories.sort(Comparator.comparing(Enum::name));

        for (FormatCategory formatCategory : formatCategories) {
            result.keySet().stream().filter(f -> f.getCategory() == formatCategory).
                    sorted(Comparator.comparing(Format::name)).forEach(format -> {
                result.get(format).sort(Comparator.comparing(Format::name));
                r.put(format, result.get(format));
            });
        }

        FormatMapUtils.print(r);

        System.out.println("OK");
    }

    public static void print(Map<Format, List<Format>> formatMap) {
        formatMap.forEach((formats, formats2) -> {
            switch (formats.getCategory()) {
                case DOCUMENTS:
                    System.out.print("documents");
                    break;
                case IMAGES:
                    System.out.print("images");
                    break;
                case WEB:
                    System.out.print("web");
                    break;
                case VIDEO:
                    System.out.print("videos");
                    break;
                case AUDIO:
                    System.out.print("audios");
                    break;
            }
            System.out.print(".put(List.of(");
            System.out.print(formats);
            System.out.print("), List.of(");
            for (Iterator<Format> iterator = formats2.iterator(); iterator.hasNext(); ) {
                System.out.print(iterator.next());
                if (iterator.hasNext()) {
                    System.out.print(", ");
                }
            }
            System.out.print("));");
            System.out.println();
        });
    }
}

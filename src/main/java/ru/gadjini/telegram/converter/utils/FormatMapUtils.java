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

    public static Map<FormatCategory, Map<List<Format>, List<Format>>> validateAndPrint(Set<Any2AnyConverter> converters) {
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

        Map<FormatCategory, Map<List<Format>, List<Format>>> formatCategory = new HashMap<>();
        print(r, formatCategory);

        return formatCategory;
    }

    private static void print(Map<Format, List<Format>> formatMap, Map<FormatCategory, Map<List<Format>, List<Format>>> formatCategory) {
        formatMap.forEach((format, formats2) -> {
            formatCategory.putIfAbsent(format.getCategory(), new HashMap<>());
            formatCategory.get(format.getCategory()).put(List.of(format), formats2);
        });
    }
}

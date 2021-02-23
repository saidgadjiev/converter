package ru.gadjini.telegram.converter.service.conversion.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversionFormatService {

    public static final String TAG = "format";

    private Map<FormatCategory, Map<List<Format>, List<Format>>> formats;

    @Autowired
    public ConversionFormatService(Map<FormatCategory, Map<List<Format>, List<Format>>> formats) {
        this.formats = formats;
    }

    public List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<FormatCategory, Map<List<Format>, List<Format>>> categoryEntry : formats.entrySet()) {
            for (Map.Entry<List<Format>, List<Format>> entry : categoryEntry.getValue().entrySet()) {
                if (entry.getKey().contains(srcFormat)) {
                    return entry.getValue().stream().filter(Format::isUserSelectable).collect(Collectors.toList());
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
}

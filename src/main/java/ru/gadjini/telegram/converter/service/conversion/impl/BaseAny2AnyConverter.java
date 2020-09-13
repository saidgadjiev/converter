package ru.gadjini.telegram.converter.service.conversion.impl;

import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class BaseAny2AnyConverter implements Any2AnyConverter {

    private final Map<List<Format>, List<Format>> map;

    protected BaseAny2AnyConverter(Map<List<Format>, List<Format>> map) {
        this.map = map;
    }

    @Override
    public final boolean accept(Format format, Format targetFormat) {
        return isConvertAvailable(format, targetFormat);
    }

    private boolean isConvertAvailable(Format src, Format target) {
        return getTargetFormats(src).contains(target);
    }

    private List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<List<Format>, List<Format>> entry : map.entrySet()) {
            if (entry.getKey().contains(srcFormat)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }
}

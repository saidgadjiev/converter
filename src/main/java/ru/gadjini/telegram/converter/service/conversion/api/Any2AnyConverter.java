package ru.gadjini.telegram.converter.service.conversion.api;

import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;

public interface Any2AnyConverter<T extends ConvertResult> {

    T convert(ConversionQueueItem fileQueueItem);

    boolean accept(Format format, Format targetFormat);
}

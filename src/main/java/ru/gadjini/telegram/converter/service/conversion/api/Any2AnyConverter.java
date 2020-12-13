package ru.gadjini.telegram.converter.service.conversion.api;

import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

public interface Any2AnyConverter {

    int createDownloads(ConversionQueueItem conversionQueueItem);

    ConvertResult convert(ConversionQueueItem fileQueueItem);

    boolean accept(Format format, Format targetFormat);

    Map<List<Format>, List<Format>> getConversionMap();
}

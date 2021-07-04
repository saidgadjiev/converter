package ru.gadjini.telegram.converter.service.conversion.api;

import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public interface Any2AnyConverter {

    int createDownloads(ConversionQueueItem conversionQueueItem);

    ConversionResult convert(ConversionQueueItem fileQueueItem, Supplier<Boolean> cancelChecker, Supplier<Boolean> canceledByUserChecker);

    boolean accept(Format format, Format targetFormat);

    Map<List<Format>, List<Format>> getConversionMap();

    default boolean needToSendProgressMessage(ConversionQueueItem conversionQueueItem, AtomicInteger progressMessageId) {
        return true;
    }
}

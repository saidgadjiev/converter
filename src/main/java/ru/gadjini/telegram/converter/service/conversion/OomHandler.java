package ru.gadjini.telegram.converter.service.conversion;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.device.BusyConvertResult;
import ru.gadjini.telegram.smart.bot.commons.service.queue.WorkQueueService;

@Component
public class OomHandler {

    private WorkQueueService queueService;

    @Autowired
    public OomHandler(WorkQueueService queueService) {
        this.queueService = queueService;
    }

    public boolean isOom(Throwable e) {
        int i = ExceptionUtils.indexOfThrowable(e, OutOfMemoryError.class);

        return i != -1;
    }

    public ConversionResult handleOom(ConversionQueueItem conversionQueueItem, Throwable e) {
        queueService.setException(conversionQueueItem.getId(), e);

        return new BusyConvertResult();
    }
}

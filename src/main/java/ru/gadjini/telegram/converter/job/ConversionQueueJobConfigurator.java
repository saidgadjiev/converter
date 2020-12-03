package ru.gadjini.telegram.converter.job;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.CorruptedFileException;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueJobConfigurator;

@Component
public class ConversionQueueJobConfigurator implements QueueJobConfigurator<ConversionQueueItem> {

    @Override
    public String getErrorCode(Throwable e) {
        if (e instanceof CorruptedFileException || e instanceof ProcessException) {
            return MessagesProperties.MESSAGE_DAMAGED_FILE;
        }

        return MessagesProperties.MESSAGE_CONVERSION_FAILED;
    }
}

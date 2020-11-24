package ru.gadjini.telegram.converter.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.domain.UploadQueueItem;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.UploadCompleted;

@Component
public class UploadJobEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadJobEventListener.class);

    private ConversionQueueService conversionQueueService;

    @Autowired
    public UploadJobEventListener(ConversionQueueService conversionQueueService) {
        this.conversionQueueService = conversionQueueService;
    }

    @EventListener
    public void uploadCompleted(UploadCompleted uploadCompleted) {
        SendFileResult sendFileResult = uploadCompleted.getSendFileResult();
        UploadQueueItem queueItem = uploadCompleted.getUploadQueueItem();
        try {
            LOGGER.debug("Result({}, {})", queueItem.getProducerId(), sendFileResult.getFileId());
            conversionQueueService.setResultFileId(queueItem.getProducerId(), sendFileResult.getFileId());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}

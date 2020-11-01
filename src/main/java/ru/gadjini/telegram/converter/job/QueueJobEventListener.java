package ru.gadjini.telegram.converter.job;

import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposeExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.QueueJobInitialization;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.QueueJobShuttingDown;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.TaskCanceled;

@Component
public class QueueJobEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionWorkerFactory.class);

    private AsposeExecutorService asposeExecutorService;

    @Autowired
    public QueueJobEventListener(AsposeExecutorService asposeExecutorService) {
        this.asposeExecutorService = asposeExecutorService;
    }

    @EventListener
    public void jobInitialization(QueueJobInitialization event) {
        applyAsposeLicenses();
    }

    @EventListener
    public void afterTaskCanceled(TaskCanceled event) {
        asposeExecutorService.cancel(event.getQueueItem().getId());
    }

    @EventListener
    public void jobShuttingDown(QueueJobShuttingDown event) {
        asposeExecutorService.shutdown();
    }

    private void applyAsposeLicenses() {
        try {
            new License().setLicense("license/license-19.lic");
            LOGGER.debug("Word license applied");

            new com.aspose.pdf.License().setLicense("license/license-19.lic");
            LOGGER.debug("Pdf license applied");

            new com.aspose.imaging.License().setLicense("license/license-19.lic");
            LOGGER.debug("Imaging license applied");

            new com.aspose.slides.License().setLicense("license/license-19.lic");
            LOGGER.debug("Slides license applied");

            new com.aspose.cells.License().setLicense("license/license-19.lic");
            LOGGER.debug("Cells license applied");
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}

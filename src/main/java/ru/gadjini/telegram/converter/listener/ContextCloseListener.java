package ru.gadjini.telegram.converter.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.file.FileManager;

@Component
public class ContextCloseListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextCloseListener.class);

    private ConvertionService conversionService;

    private FileManager fileManager;

    public ContextCloseListener(ConvertionService conversionService, FileManager fileManager) {
        this.conversionService = conversionService;
        this.fileManager = fileManager;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            conversionService.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown conversionService. " + e.getMessage(), e);
        }
        try {
            fileManager.cancelDownloads();
        } catch (Throwable e) {
            LOGGER.error("Error cancel downloading telegramService. " + e.getMessage(), e);
        }
        LOGGER.debug("Shutdown success");
    }
}

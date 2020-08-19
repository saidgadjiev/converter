package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private ConvertionService conversionService;

    @Autowired
    public void setConversionService(ConvertionService conversionService) {
        this.conversionService = conversionService;
    }

    @Bean
    @Qualifier("conversionTaskExecutor")
    public SmartExecutorService conversionTaskExecutor() {
        SmartExecutorService executorService = new SmartExecutorService();
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(4, 4,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                (r, executor) -> {
                    executorService.complete(((SmartExecutorService.Job) r).getId());
                    conversionService.rejectTask((SmartExecutorService.Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                SmartExecutorService.Job poll = conversionService.getTask(SmartExecutorService.JobWeight.LIGHT);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(4, 4,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                (r, executor) -> {
                    executorService.complete(((SmartExecutorService.Job) r).getId());
                    conversionService.rejectTask((SmartExecutorService.Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                SmartExecutorService.Job poll = conversionService.getTask(SmartExecutorService.JobWeight.HEAVY);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };

        LOGGER.debug("Conversion light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Conversion heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(SmartExecutorService.JobWeight.LIGHT, lightTaskExecutor, SmartExecutorService.JobWeight.HEAVY, heavyTaskExecutor));
    }
}

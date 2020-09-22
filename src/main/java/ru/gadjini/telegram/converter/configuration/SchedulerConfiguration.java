package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
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
    public SmartExecutorService conversionTaskExecutor(UserService userService, FileManager fileManager,
                                                       @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService) {
        SmartExecutorService executorService = new SmartExecutorService(messageService, localisationService, fileManager, userService);
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(4, 4,
                0, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                (r, executor) -> {
                    SmartExecutorService.Job job = getJob(r);
                    executorService.complete(job.getId());
                    conversionService.rejectTask(job);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                SmartExecutorService.Job poll = conversionService.getTask(SmartExecutorService.JobWeight.LIGHT);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(2, 2,
                0, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                (r, executor) -> {
                    SmartExecutorService.Job job = getJob(r);
                    executorService.complete(job.getId());
                    conversionService.rejectTask(job);
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

    private SmartExecutorService.Job getJob(Runnable runnable) {
        try {
            Field field = runnable.getClass().getDeclaredField("callable");
            field.setAccessible(true);
            Object callbable = field.get(runnable);
            Field task = callbable.getClass().getDeclaredField("task");
            task.setAccessible(true);

            return (SmartExecutorService.Job) task.get(callbable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

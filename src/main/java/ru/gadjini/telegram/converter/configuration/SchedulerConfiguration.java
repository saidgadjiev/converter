package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.job.ConversionJob;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private ConversionJob conversionJob;

    @Autowired
    public void setConversionJob(ConversionJob conversionJob) {
        this.conversionJob = conversionJob;
    }

    @Bean
    @Qualifier("conversionTaskExecutor")
    public SmartExecutorService conversionTaskExecutor(UserService userService, FileManager fileManager,
                                                       @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService) {
        SmartExecutorService executorService = new SmartExecutorService(messageService, localisationService, fileManager, userService);
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new SynchronousQueue<>());
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, new SynchronousQueue<>());

        LOGGER.debug("Conversion light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Conversion heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        executorService.setExecutors(Map.of(SmartExecutorService.JobWeight.LIGHT, lightTaskExecutor, SmartExecutorService.JobWeight.HEAVY, heavyTaskExecutor));
        executorService.setRejectJobHandler(SmartExecutorService.JobWeight.HEAVY, job -> conversionJob.rejectTask(job));
        executorService.setRejectJobHandler(SmartExecutorService.JobWeight.LIGHT, job -> conversionJob.rejectTask(job));

        return executorService;
    }
}

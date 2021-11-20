package ru.gadjini.telegram.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;

import javax.annotation.PostConstruct;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@ConfigurationPropertiesScan("ru.gadjini.telegram")
@EnableScheduling
@SpringBootApplication
@ComponentScan("ru")
public class ConverterApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterApplication.class);

    private ApplicationProperties applicationProperties;

    public static void main(String[] args) {
        setDefaultLocaleAndTZ();
        try {
            SpringApplication.run(ConverterApplication.class, args);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    @Autowired
    public void setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void init() {
        int mb = 1024 * 1024;
        LOGGER.debug("Converter({})", applicationProperties.getConverter());
        LOGGER.debug("Default zone({})", TimeZone.getDefault().getDisplayName());
        LOGGER.debug("Default locale({})", Locale.getDefault().getDisplayName());
        LOGGER.debug("Heap size({}mb)", Runtime.getRuntime().totalMemory() / mb);
        LOGGER.debug("Max heap size({}mb)", Runtime.getRuntime().maxMemory() / mb);
        LOGGER.debug("Available processors({})", Runtime.getRuntime().availableProcessors());
    }

    private static void setDefaultLocaleAndTZ() {
        Locale.setDefault(new Locale(LocalisationService.EN_LOCALE));
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @Autowired
    private FFprobeDevice fFprobeDevice;

    @Override
    public void run(String... args) throws Exception {
        List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams("C:/mm.mkv");

        System.out.println(allStreams);
    }
}

package ru.gadjini.telegram.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.property.DetectLanguageProperties;
import ru.gadjini.telegram.smart.bot.commons.property.BotApiProperties;
import ru.gadjini.telegram.smart.bot.commons.property.MTProtoProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

@EnableConfigurationProperties(value = {
        ConversionProperties.class,
        DetectLanguageProperties.class,
        MTProtoProperties.class,
        BotApiProperties.class
})
@EnableScheduling
@SpringBootApplication
@ComponentScan("ru")
public class ConverterApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterApplication.class);

    public static void main(String[] args) {
        setDefaultLocaleAndTZ();
        try {
            SpringApplication.run(ConverterApplication.class, args);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw ex;
        }
        startLogs();
    }

    private static void startLogs() {
        int mb = 1024 * 1024;
        LOGGER.debug("Default zone({})", TimeZone.getDefault());
        LOGGER.debug("Default locale({})", Locale.getDefault());
        LOGGER.debug("Heap size({}mb)", Runtime.getRuntime().totalMemory() / mb);
        LOGGER.debug("Max heap size({}mb)", Runtime.getRuntime().maxMemory() / mb);
    }

    private static void setDefaultLocaleAndTZ() {
        Locale.setDefault(new Locale(LocalisationService.EN_LOCALE));
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }
}

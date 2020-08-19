package ru.gadjini.telegram.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.gadjini.telegram.converter.property.BotApiProperties;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.property.DetectLanguageProperties;
import ru.gadjini.telegram.converter.property.MTProtoProperties;
import ru.gadjini.telegram.converter.service.LocalisationService;

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
    }

    private static void setDefaultLocaleAndTZ() {
        Locale.setDefault(new Locale(LocalisationService.EN_LOCALE));
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }
}

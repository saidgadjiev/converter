package ru.gadjini.telegram.converter.property;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;

import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties
public class ApplicationProperties {

    @Value("${converter:all}")
    private String converter;

    public String getConverter() {
        return converter;
    }

    public void setConverter(String converter) {
        this.converter = converter;
    }

    public boolean is(String conv) {
        return converter.equals(conv) || converter.equals(FormatsConfiguration.ALL_CONVERTER);
    }

    public Set<String> getConverters() {
        Set<String> converters = new HashSet<>();

        if (converter.equals(FormatsConfiguration.ALL_CONVERTER)) {
            converters.add(FormatsConfiguration.ALL_CONVERTER);
            converters.add(FormatsConfiguration.AUDIO_CONVERTER);
            converters.add(FormatsConfiguration.VIDEO_CONVERTER);
            converters.add(FormatsConfiguration.DOCUMENT_CONVERTER);
        } else {
            converters.add(converter);
        }

        return converters;
    }
}

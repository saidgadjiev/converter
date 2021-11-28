package ru.gadjini.telegram.converter.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.utils.FormatMapUtils;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class FormatsConfiguration {

    public static final String ALL_CONVERTER = "all";

    public static final String DOCUMENT_CONVERTER = "document";

    public static final String VIDEO_CONVERTER = "video";

    public static final String AUDIO_CONVERTER = "audio";

    private final Map<FormatCategory, Map<List<Format>, List<Format>>> formats;

    @Autowired
    public FormatsConfiguration(Set<Any2AnyConverter> converters) {
        this.formats = FormatMapUtils.validateAndPrint(converters);
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = VIDEO_CONVERTER
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> videoFormats() {
        return Map.of(FormatCategory.VIDEO, formats.get(FormatCategory.VIDEO));
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = AUDIO_CONVERTER
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> audioFormats() {
        return Map.of(FormatCategory.AUDIO, formats.get(FormatCategory.AUDIO));
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = DOCUMENT_CONVERTER
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> documentFormats() {
        return Map.of(
                FormatCategory.DOCUMENTS, formats.get(FormatCategory.DOCUMENTS),
                FormatCategory.IMAGES, formats.get(FormatCategory.IMAGES),
                FormatCategory.WEB, formats.get(FormatCategory.WEB)
        );
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = ALL_CONVERTER,
            matchIfMissing = true
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> allFormats() {
        return formats;
    }
}

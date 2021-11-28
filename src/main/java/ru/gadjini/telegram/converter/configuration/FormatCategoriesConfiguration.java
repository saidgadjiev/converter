package ru.gadjini.telegram.converter.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.Set;

@Configuration
public class FormatCategoriesConfiguration {

    private ApplicationProperties applicationProperties;

    @Autowired
    public FormatCategoriesConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    public Set<FormatCategory> formatCategories() {
        if (applicationProperties.is(FormatsConfiguration.AUDIO_CONVERTER)) {
            return Set.of(FormatCategory.AUDIO);
        }
        if (applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER)) {
            return Set.of(FormatCategory.VIDEO);
        }
        if (applicationProperties.is(FormatsConfiguration.DOCUMENT_CONVERTER)) {
            return Set.of(FormatCategory.WEB, FormatCategory.DOCUMENTS, FormatCategory.IMAGES);
        }

        return Set.of(FormatCategory.COMMON, FormatCategory.IMAGES, FormatCategory.WEB,
                FormatCategory.DOCUMENTS, FormatCategory.AUDIO, FormatCategory.VIDEO);
    }
}

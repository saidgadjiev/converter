package ru.gadjini.telegram.converter.command.bot.cut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.Locale;

@Component
public class VideoCutConfigurator implements CutMediaConfigurator {

    private ApplicationProperties applicationProperties;

    private LocalisationService localisationService;

    @Autowired
    public VideoCutConfigurator(ApplicationProperties applicationProperties, LocalisationService localisationService) {
        this.applicationProperties = applicationProperties;
        this.localisationService = localisationService;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER);
    }

    @Override
    public String getMediaTypeName(Locale locale) {
        return localisationService.getMessage(ConverterMessagesProperties.VIDEO_MEDIA_TYPE_NAME, locale);
    }

    @Override
    public FormatCategory getFormatCategory() {
        return FormatCategory.VIDEO;
    }

    @Override
    public String getCommandName() {
        return ConverterCommandNames.CUT_VIDEO;
    }
}

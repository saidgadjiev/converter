package ru.gadjini.telegram.converter.command.bot.merge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.Locale;

@Component
public class MergeVideoFilesConfigurator implements MergeFilesConfigurator {

    private LocalisationService localisationService;

    private ApplicationProperties applicationProperties;

    @Autowired
    public MergeVideoFilesConfigurator(LocalisationService localisationService, ApplicationProperties applicationProperties) {
        this.localisationService = localisationService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getMediaTypeName(Locale locale) {
        return localisationService.getMessage(ConverterMessagesProperties.VIDEO_MEDIA_TYPE_NAME, locale);
    }

    @Override
    public boolean isValidFormat(Format format) {
        return format.getCategory() == FormatCategory.VIDEO;
    }

    @Override
    public String getCommandName() {
        return ConverterCommandNames.CONCATENATE_VIDEOS;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER);
    }

    @Override
    public Format getTargetFormat() {
        return Format.MERGE;
    }

    @Override
    public int getMaxFiles() {
        return 2;
    }
}

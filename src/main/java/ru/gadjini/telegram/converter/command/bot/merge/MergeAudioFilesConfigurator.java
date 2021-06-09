package ru.gadjini.telegram.converter.command.bot.merge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

@Component
public class MergeAudioFilesConfigurator implements MergeFilesConfigurator {

    private ApplicationProperties applicationProperties;

    @Autowired
    public MergeAudioFilesConfigurator(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getFileType() {
        return FormatCategory.AUDIO.name().toLowerCase();
    }

    @Override
    public boolean isValidFormat(Format format) {
        return format.getCategory() == FormatCategory.AUDIO;
    }

    @Override
    public String getCommandName() {
        return ConverterCommandNames.CONCATENATE_AUDIOS;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.AUDIO_CONVERTER);
    }

    @Override
    public Format getTargetFormat() {
        return Format.MERGE;
    }
}

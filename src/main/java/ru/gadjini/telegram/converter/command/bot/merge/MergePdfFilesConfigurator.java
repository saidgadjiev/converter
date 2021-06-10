package ru.gadjini.telegram.converter.command.bot.merge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

@Component
public class MergePdfFilesConfigurator implements MergeFilesConfigurator {

    private ApplicationProperties applicationProperties;

    @Autowired
    public MergePdfFilesConfigurator(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getFileType() {
        return Format.PDF.getName().toLowerCase();
    }

    @Override
    public boolean isValidFormat(Format format) {
        return format == Format.PDF;
    }

    @Override
    public String getCommandName() {
        return ConverterCommandNames.MERGE_PDFS;
    }

    @Override
    public boolean accept(Message message) {
        return applicationProperties.is(FormatsConfiguration.DOCUMENT_CONVERTER);
    }

    @Override
    public Format getTargetFormat() {
        return Format.MERGE_PDFS;
    }

    @Override
    public int getMaxFiles() {
        return 10;
    }
}

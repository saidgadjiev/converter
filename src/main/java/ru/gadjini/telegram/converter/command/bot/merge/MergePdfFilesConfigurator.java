package ru.gadjini.telegram.converter.command.bot.merge;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

@Component
public class MergePdfFilesConfigurator implements MergeFilesConfigurator {

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
}

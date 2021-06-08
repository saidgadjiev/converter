package ru.gadjini.telegram.converter.command.bot.merge;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

@Component
public class MergeAudioFilesConfigurator implements MergeFilesConfigurator {

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
        return ConverterCommandNames.MERGE_AUDIOS;
    }
}

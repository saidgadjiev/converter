package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.service.CommandMessageBuilder;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;

import java.util.Locale;

@Service
public class ConverterCommandMessageBuilder implements CommandMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public ConverterCommandMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String getCommandsInfo(Locale locale) {
        StringBuilder info = new StringBuilder();

        info.append("/").append(CommandNames.START_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.START_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(ConverterCommandNames.MERGE_PDFS).append(" - ").append(localisationService.getMessage(MessagesProperties.MERGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.LANGUAGE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(ConverterCommandNames.FORMATS_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.FORMATS_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.HELP_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.HELP_COMMAND_DESCRIPTION, locale));

        return info.toString();
    }
}

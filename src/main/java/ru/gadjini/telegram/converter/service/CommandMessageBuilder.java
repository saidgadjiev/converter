package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;

import java.util.Locale;

@Service
public class CommandMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public CommandMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String getCommandsInfo(Locale locale) {
        StringBuilder info = new StringBuilder();

        info.append("/").append(CommandNames.START_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.START_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.CONVERT_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.CONVERT_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.LANGUAGE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.FORMATS_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.FORMATS_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.HELP_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.HELP_COMMAND_DESCRIPTION, locale));

        return info.toString();
    }
}

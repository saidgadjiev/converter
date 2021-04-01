package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.service.CommandMessageBuilder;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandParser;

import java.util.Locale;
import java.util.Set;

@Service
public class ConverterCommandMessageBuilder implements CommandMessageBuilder {

    private LocalisationService localisationService;

    @Value("${converter:all}")
    private String converter;

    @Autowired
    public ConverterCommandMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String getCommandsInfo(Locale locale) {
        StringBuilder info = new StringBuilder();

        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.START_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.START_COMMAND_DESCRIPTION, locale)).append("\n");
        if (Set.of(FormatsConfiguration.DOCUMENT_CONVERTER, FormatsConfiguration.ALL_CONVERTER).contains(converter)) {
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.MERGE_PDFS).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.MERGE_COMMAND_DESCRIPTION, locale)).append("\n");
        }
        if (Set.of(FormatsConfiguration.AUDIO_CONVERTER, FormatsConfiguration.ALL_CONVERTER).contains(converter)) {
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.COMPRESS_AUDIO).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.COMPRESS_AUDIO_COMMAND_DESCRIPTION, locale)).append("\n");
        }
        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.SUBSCRIPTION).append(" - ").append(localisationService.getMessage(MessagesProperties.SUBSCRIPTION_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.TIME_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.BOT_TIME_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.LANGUAGE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.FORMATS_COMMAND).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.FORMATS_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.HELP_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.HELP_COMMAND_DESCRIPTION, locale));

        return info.toString();
    }
}

package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.property.SubscriptionProperties;
import ru.gadjini.telegram.smart.bot.commons.service.CommandMessageBuilder;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandParser;

import java.util.Locale;

@Service
public class ConverterCommandMessageBuilder implements CommandMessageBuilder {

    private LocalisationService localisationService;

    private ApplicationProperties applicationProperties;

    private SubscriptionProperties subscriptionProperties;

    @Autowired
    public ConverterCommandMessageBuilder(LocalisationService localisationService, ApplicationProperties applicationProperties,
                                          SubscriptionProperties subscriptionProperties) {
        this.localisationService = localisationService;
        this.applicationProperties = applicationProperties;
        this.subscriptionProperties = subscriptionProperties;
    }

    public String getCommandsInfo(Locale locale) {
        StringBuilder info = new StringBuilder();

        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.START_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.START_COMMAND_DESCRIPTION, locale)).append("\n");
        if (applicationProperties.is(FormatsConfiguration.DOCUMENT_CONVERTER)) {
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.MERGE_PDFS).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.CONCATENATE_COMMAND_DESCRIPTION, locale)).append("\n");
        }
        if (applicationProperties.is(FormatsConfiguration.AUDIO_CONVERTER)) {
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.COMPRESS_AUDIO).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.COMPRESS_AUDIO_COMMAND_DESCRIPTION, locale)).append("\n");
        }
        if (applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER)) {
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.EDIT_VIDEO).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.VEDIT_COMMAND_DESCRIPTION, locale)).append("\n");
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.VAVMERGE).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.VAVMERGE_COMMAND_DESCRIPTION, locale)).append("\n");
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.VAIMAKE).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.VAIMAKE_COMMAND_DESCRIPTION, locale)).append("\n");
            info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.CUT_VIDEO).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.CUT_VIDEO_COMMAND_DESCRIPTION, locale)).append("\n");
        }
        if (subscriptionProperties.isCheckPaidSubscription()) {
            info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.SUBSCRIPTION).append(" - ").append(localisationService.getMessage(MessagesProperties.SUBSCRIPTION_COMMAND_DESCRIPTION, locale)).append("\n");
            info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.REFRESH_SUBSCRIPTION).append(" - ").append(localisationService.getMessage(MessagesProperties.REFRESH_SUBSCRIPTION_COMMAND_DESCRIPTION, locale)).append("\n");
        }
        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.TIME_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.BOT_TIME_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.LANGUAGE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append(CommandParser.COMMAND_START_CHAR).append(ConverterCommandNames.FORMATS_COMMAND).append(" - ").append(localisationService.getMessage(ConverterMessagesProperties.FORMATS_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append(CommandParser.COMMAND_START_CHAR).append(CommandNames.HELP_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.HELP_COMMAND_DESCRIPTION, locale));

        if (applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER)) {
            info.append("\n\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_NON_COMMAND_FEATURES_HEADER, locale))
                    .append("\n").append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_NON_COMMAND_FEATURES, locale));
        }

        return info.toString();
    }
}

package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.gadjini.telegram.converter.command.bot.cut.CutAudioConfigurator;
import ru.gadjini.telegram.converter.command.bot.cut.CutMediaCommand;
import ru.gadjini.telegram.converter.command.bot.cut.VideoCutConfigurator;
import ru.gadjini.telegram.converter.command.bot.merge.MergeAudioFilesConfigurator;
import ru.gadjini.telegram.converter.command.bot.merge.MergeFilesCommand;
import ru.gadjini.telegram.converter.command.bot.merge.MergePdfFilesConfigurator;
import ru.gadjini.telegram.converter.property.Url2PdfServerProperties;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.keyboard.ConverterReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.filter.*;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.ChannelSubscriptionFilter;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.PaidSubscriptionFilter;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

@Configuration
public class BotConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfiguration.class);

    @Autowired
    public BotConfiguration(Url2PdfServerProperties conversionProperties) {
        LOGGER.debug("Conversion time out({})", conversionProperties.getTimeOut());
    }

    @Bean
    public BotFilter botFilter(UpdateFilter updateFilter, UserSynchronizedFilter userSynchronizedFilter,
                               StartCommandFilter startCommandFilter, TechWorkFilter techWorkFilter,
                               MediaFilter mediaFilter, LastActivityFilter activityFilter,
                               ChannelSubscriptionFilter subscriptionFilter, UpdatesHandlerFilter updatesHandlerFilter,
                               PaidSubscriptionFilter paidSubscriptionFilter, DistributionFilter distributionFilter) {
        updateFilter.setNext(userSynchronizedFilter)
                .setNext(startCommandFilter).setNext(subscriptionFilter).setNext(activityFilter)
                .setNext(distributionFilter).setNext(mediaFilter)
                .setNext(techWorkFilter).setNext(paidSubscriptionFilter).setNext(updatesHandlerFilter);

        return updateFilter;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public MergeFilesCommand mergePdfFilesCommand(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                                                  UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                                  MessageMediaService messageMediaService, CommandStateService commandStateService,
                                                  ConvertionService convertionService,
                                                  MergePdfFilesConfigurator mergeFilesConfigurator) {
        return new MergeFilesCommand(messageService, localisationService, userService, replyKeyboardService, messageMediaService,
                commandStateService, convertionService, mergeFilesConfigurator);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public MergeFilesCommand mergeAudioFilesCommand(@TgMessageLimitsControl MessageService messageService, LocalisationService localisationService,
                                                    UserService userService, @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                                    MessageMediaService messageMediaService, CommandStateService commandStateService,
                                                    ConvertionService convertionService,
                                                    MergeAudioFilesConfigurator mergeAudioFilesConfigurator) {
        return new MergeFilesCommand(messageService, localisationService, userService, replyKeyboardService, messageMediaService,
                commandStateService, convertionService, mergeAudioFilesConfigurator);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CutMediaCommand videoCutMediaCommand(VideoCutConfigurator videoCutConfigurator,
                                                @TgMessageLimitsControl MessageService messageService,
                                                UserService userService,
                                                LocalisationService localisationService,
                                                @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                                CommandStateService commandStateService,
                                                MessageMediaService messageMediaService,
                                                ConvertionService convertionService) {
        return new CutMediaCommand(messageService, userService, localisationService, replyKeyboardService,
                commandStateService, messageMediaService, convertionService, videoCutConfigurator);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CutMediaCommand audioCutMediaCommand(CutAudioConfigurator cutAudioConfigurator,
                                                @TgMessageLimitsControl MessageService messageService,
                                                UserService userService,
                                                LocalisationService localisationService,
                                                @KeyboardHolder ConverterReplyKeyboardService replyKeyboardService,
                                                CommandStateService commandStateService,
                                                MessageMediaService messageMediaService,
                                                ConvertionService convertionService) {
        return new CutMediaCommand(messageService, userService, localisationService, replyKeyboardService,
                commandStateService, messageMediaService, convertionService, cutAudioConfigurator);
    }
}

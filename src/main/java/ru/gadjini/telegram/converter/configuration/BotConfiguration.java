package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.filter.*;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.smart.bot.commons.filter.*;

@Configuration
public class BotConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfiguration.class);

    @Autowired
    public BotConfiguration(ConversionProperties conversionProperties) {
        LOGGER.debug("Calibre time out({})", conversionProperties.getCalibreLongConversionTimeOut());
        LOGGER.debug("Pdf to word time out({})", conversionProperties.getCalibreLongConversionTimeOut());
    }

    @Bean
    public BotFilter botFilter(ConverterBotFilter any2AnyBotFilter,
                               UpdateFilter updateFilter, StartCommandFilter startCommandFilter,
                               MediaFilter mediaFilter, LastActivityFilter activityFilter,
                               SubscriptionFilter subscriptionFilter) {
        updateFilter.setNext(mediaFilter).setNext(startCommandFilter).setNext(subscriptionFilter)
                .setNext(activityFilter).setNext(any2AnyBotFilter);
        return updateFilter;
    }
}

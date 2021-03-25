package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.smart.bot.commons.filter.*;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.ChannelSubscriptionFilter;

@Configuration
public class BotConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfiguration.class);

    @Autowired
    public BotConfiguration(ConversionProperties conversionProperties) {
        LOGGER.debug("Calibre time out({})", conversionProperties.getCalibreLongConversionTimeOut());
        LOGGER.debug("Pdf to word time out({})", conversionProperties.getCalibreLongConversionTimeOut());
    }

    @Bean
    public BotFilter botFilter(UpdateFilter updateFilter, StartCommandFilter startCommandFilter,
                               MediaFilter mediaFilter, LastActivityFilter activityFilter,
                               ChannelSubscriptionFilter subscriptionFilter, UpdatesHandlerFilter updatesHandlerFilter) {
        updateFilter.setNext(mediaFilter).setNext(startCommandFilter).setNext(subscriptionFilter)
                .setNext(activityFilter).setNext(updatesHandlerFilter);

        return updateFilter;
    }
}

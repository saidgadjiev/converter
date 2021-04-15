package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.smart.bot.commons.filter.*;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.ChannelSubscriptionFilter;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.PaidSubscriptionFilter;

@Configuration
public class BotConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfiguration.class);

    @Autowired
    public BotConfiguration(ConversionProperties conversionProperties) {
        LOGGER.debug("Conversion time out({})", conversionProperties.getConversionTimeOut());
    }

    @Bean
    public BotFilter botFilter(UpdateFilter updateFilter, UserSynchronizedFilter userSynchronizedFilter,
                               StartCommandFilter startCommandFilter, TechWorkFilter techWorkFilter,
                               MediaFilter mediaFilter, LastActivityFilter activityFilter,
                               ChannelSubscriptionFilter subscriptionFilter, UpdatesHandlerFilter updatesHandlerFilter,
                               PaidSubscriptionFilter paidSubscriptionFilter) {
        updateFilter.setNext(userSynchronizedFilter).setNext(activityFilter).setNext(mediaFilter)
                .setNext(startCommandFilter).setNext(subscriptionFilter)
                .setNext(techWorkFilter).setNext(paidSubscriptionFilter).setNext(updatesHandlerFilter);

        return updateFilter;
    }
}

package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.property.Url2PdfServerProperties;
import ru.gadjini.telegram.smart.bot.commons.filter.*;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.ChannelSubscriptionFilter;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.PaidSubscriptionFilter;

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
}

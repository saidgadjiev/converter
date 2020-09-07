package ru.gadjini.telegram.converter.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.converter.filter.*;
import ru.gadjini.telegram.smart.bot.commons.filter.*;

@Configuration
public class BotConfiguration {

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

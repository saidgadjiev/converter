package ru.gadjini.telegram.converter.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;
import ru.gadjini.telegram.converter.service.Any2AnyBotService;

@Component
public class Any2AnyBotFilter extends BaseBotFilter {

    private Any2AnyBotService any2AnyBotService;

    @Autowired
    public Any2AnyBotFilter(Any2AnyBotService any2AnyBotService) {
        this.any2AnyBotService = any2AnyBotService;
    }

    @Override
    public void doFilter(Update update) {
        any2AnyBotService.onUpdateReceived(update);
    }
}

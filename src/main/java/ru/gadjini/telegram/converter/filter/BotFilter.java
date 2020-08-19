package ru.gadjini.telegram.converter.filter;

import ru.gadjini.telegram.converter.model.bot.api.object.Update;

public interface BotFilter {

    BotFilter setNext(BotFilter next);

    void doFilter(Update update);
}

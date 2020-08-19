package ru.gadjini.telegram.converter.filter;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;

@Component
public class UpdateFilter extends BaseBotFilter {

    @Override
    public void doFilter(Update update) {
        if (update.hasMessage() || update.hasCallbackQuery()) {
            super.doFilter(update);
        }
    }
}

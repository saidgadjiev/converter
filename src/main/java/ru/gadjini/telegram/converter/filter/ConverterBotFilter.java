package ru.gadjini.telegram.converter.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gadjini.telegram.converter.service.ConverterBotService;
import ru.gadjini.telegram.smart.bot.commons.filter.BaseBotFilter;

@Component
public class ConverterBotFilter extends BaseBotFilter {

    private ConverterBotService converterBotService;

    @Autowired
    public ConverterBotFilter(ConverterBotService converterBotService) {
        this.converterBotService = converterBotService;
    }

    @Override
    public void doFilter(Update update) {
        converterBotService.onUpdateReceived(update);
    }
}

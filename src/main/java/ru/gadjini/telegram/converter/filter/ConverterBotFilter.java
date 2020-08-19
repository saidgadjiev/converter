package ru.gadjini.telegram.converter.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;
import ru.gadjini.telegram.converter.service.ConverterBotService;

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

package ru.gadjini.telegram.converter.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.model.TgMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;
import ru.gadjini.telegram.converter.service.UserService;

@Component
public class LastActivityFilter extends BaseBotFilter {

    private UserService userService;

    @Autowired
    public LastActivityFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void doFilter(Update update) {
        userService.activity(TgMessage.getUser(update));
        super.doFilter(update);
    }
}

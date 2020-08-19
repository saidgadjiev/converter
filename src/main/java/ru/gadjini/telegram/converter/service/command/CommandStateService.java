package ru.gadjini.telegram.converter.service.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.dao.command.state.CommandStateDao;
import ru.gadjini.telegram.converter.exception.UserException;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;

import java.util.concurrent.TimeUnit;

@Service
public class CommandStateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandStateService.class);

    private static final long TTL_HOURS = 10;

    private CommandStateDao commandStateDao;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CommandStateService(@Qualifier("redis") CommandStateDao commandStateDao,
                               LocalisationService localisationService, UserService userService) {
        this.commandStateDao = commandStateDao;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public void setState(long chatId, String command, Object state) {
        commandStateDao.setState(chatId, command, state, TTL_HOURS, TimeUnit.HOURS);
    }

    public <T> T getState(long chatId, String command, boolean expiredCheck, Class<T> tClass) {
        T state = commandStateDao.getState(chatId, command, tClass);

        if (expiredCheck && state == null) {
            LOGGER.warn("State not found({}, {})", chatId, command);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SESSION_EXPIRED, userService.getLocaleOrDefault((int) chatId)));
        }

        return state;
    }

    public boolean hasState(long chatId, String command) {
        return commandStateDao.hasState(chatId, command);
    }

    public void deleteState(long chatId, String command) {
        commandStateDao.deleteState(chatId, command);
    }
}

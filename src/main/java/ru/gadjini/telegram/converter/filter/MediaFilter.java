package ru.gadjini.telegram.converter.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.exception.UserException;
import ru.gadjini.telegram.converter.model.Any2AnyFile;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;
import ru.gadjini.telegram.converter.service.FileService;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.file.FileManager;
import ru.gadjini.telegram.converter.utils.MemoryUtils;

import java.util.Locale;

import static ru.gadjini.telegram.converter.common.TgConstants.LARGE_FILE_SIZE;

@Component
@Qualifier("messagelimits")
public class MediaFilter extends BaseBotFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaFilter.class);

    private UserService userService;

    private LocalisationService localisationService;

    private FileService fileService;

    private FileManager fileManager;

    @Autowired
    public MediaFilter(UserService userService, LocalisationService localisationService, FileService fileService, FileManager fileManager) {
        this.userService = userService;
        this.localisationService = localisationService;
        this.fileService = fileService;
        this.fileManager = fileManager;
    }

    @Override
    public void doFilter(Update update) {
        if (update.hasMessage()) {
            Any2AnyFile file = fileService.getFile(update.getMessage(), Locale.getDefault());
            if (file != null) {
                checkInMediaSize(update.getMessage(), file);
                fileManager.inputFile(update.getMessage().getChatId(), file.getFileId(), file.getFileSize());
            }
        }

        super.doFilter(update);
    }

    private void checkInMediaSize(Message message, Any2AnyFile file) {
        if (file.getFileSize() > LARGE_FILE_SIZE) {
            LOGGER.warn("Large in file({}, {})", message.getFrom().getId(), file);
            throw new UserException(localisationService.getMessage(
                    MessagesProperties.MESSAGE_TOO_LARGE_IN_FILE,
                    new Object[]{MemoryUtils.humanReadableByteCount(message.getDocument().getFileSize())},
                    userService.getLocaleOrDefault(message.getFrom().getId())));
        } else if (file.getFileSize() > MemoryUtils.MB_100) {
            LOGGER.warn("Heavy file({}, {})", message.getFrom().getId(), file);
        }
    }
}

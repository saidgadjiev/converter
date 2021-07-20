package ru.gadjini.telegram.converter.service.caption;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.domain.FileSource;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;

@Component
public class CopyRightCaptionGenerator implements CaptionGenerator {

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CopyRightCaptionGenerator(LocalisationService localisationService, UserService userService) {
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public String generate(long userId, FileSource fileSource, String currentCaption) {
        if (!FileSource.URL.equals(fileSource)) {
            return currentCaption;
        }
        if (StringUtils.isBlank(currentCaption)) {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COPYRIGHT, userService.getLocaleOrDefault(userId));
        }

        return currentCaption + "\n\n"
                + localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COPYRIGHT, userService.getLocaleOrDefault(userId));
    }
}

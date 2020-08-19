package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;

import java.time.Duration;
import java.util.Locale;

@Component
public class Time2TextService {

    private LocalisationService localisationService;

    @Autowired
    public Time2TextService(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String time(Duration duration, Locale locale) {
        StringBuilder time = new StringBuilder();

        if (duration.toHoursPart() != 0) {
            time
                    .append(duration.toHoursPart()).append(" ")
                    .append(localisationService.getMessage(MessagesProperties.HOUR_PART, locale)).append(" ");
        }
        if (duration.toMinutesPart() != 0) {
            time
                    .append(duration.toMinutesPart()).append(" ")
                    .append(localisationService.getMessage(MessagesProperties.MINUTE_PART, locale)).append(" ");
        }
        if (duration.toSecondsPart() != 0) {
            time
                    .append(duration.toSecondsPart()).append(" ")
                    .append(localisationService.getMessage(MessagesProperties.SECOND_PART, locale)).append(" ");
        }

        return time.toString().trim();
    }
}

package ru.gadjini.telegram.converter.service.queue.conversion;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.conversion.api.Format;
import ru.gadjini.telegram.converter.utils.MemoryUtils;

import java.util.Locale;
import java.util.Set;

@Service
public class ConversionQueueMessageBuilder {

    private static final Set<Format> NON_DISPLAY_FORMATS = Set.of(Format.TEXT);

    private static final Set<Format> NON_SIZEABLE_FORMATS = Set.of(Format.TEXT, Format.URL);

    private LocalisationService localisationService;

    @Autowired
    public ConversionQueueMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String getChooseFormat(Set<String> warns, Locale locale) {
        StringBuilder message = new StringBuilder();
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION, locale));
        String w = warns(warns, locale);
        if (StringUtils.isNotBlank(w)) {
            message.append("\n\n").append(w);
        }

        return message.toString();
    }

    public String getQueuedMessage(ConversionQueueItem queueItem, Set<String> warns, Locale locale) {
        StringBuilder text = new StringBuilder();
        text.append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getTargetFormat().name(), queueItem.getPlaceInQueue()}, locale));

        if (!NON_DISPLAY_FORMATS.contains(queueItem.getFormat())) {
            text.append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_FORMAT, new Object[]{queueItem.getFormat().name()}, locale));
        }
        if (!NON_SIZEABLE_FORMATS.contains(queueItem.getFormat())) {
            text.append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_SIZE, new Object[]{MemoryUtils.humanReadableByteCount(queueItem.getSize())}, locale));
        }

        String w = warns(warns, locale);

        if (StringUtils.isNotBlank(w)) {
            text.append("\n\n").append(w);
        }

        return text.toString();
    }

    public String warns(Set<String> warns, Locale locale) {
        StringBuilder warnsText = new StringBuilder();
        int i = 1;
        for (String warn : warns) {
            if (warnsText.length() > 0) {
                warnsText.append("\n");
            }
            warnsText.append(i++).append(") ").append(warn);
        }

        if (warnsText.length() > 0) {
            return localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_WARNINGS, new Object[]{warnsText.toString()}, locale);
        }

        return null;
    }
}

package ru.gadjini.telegram.converter.service.queue.conversion;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.Time2TextService;
import ru.gadjini.telegram.converter.service.TimeCreator;
import ru.gadjini.telegram.converter.service.conversion.api.Format;
import ru.gadjini.telegram.converter.utils.MemoryUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ConversionQueueMessageBuilder {

    private static final Set<Format> NON_DISPLAY_FORMATS = Set.of(Format.TEXT);

    private static final Set<Format> NON_SIZEABLE_FORMATS = Set.of(Format.TEXT, Format.URL);

    private LocalisationService localisationService;

    private TimeCreator timeCreator;

    private Time2TextService time2TextService;

    @Autowired
    public ConversionQueueMessageBuilder(LocalisationService localisationService, TimeCreator timeCreator, Time2TextService time2TextService) {
        this.localisationService = localisationService;
        this.timeCreator = timeCreator;
        this.time2TextService = time2TextService;
    }

    public String getItems(List<ConversionQueueItem> queueItems, Locale locale) {
        if (queueItems.isEmpty()) {
            return localisationService.getMessage(MessagesProperties.MESSAGE_QUERIES_EMPTY, locale);
        }
        StringBuilder message = new StringBuilder();
        int i = 1;
        for (ConversionQueueItem fileQueueItem : queueItems) {
            if (message.length() > 0) {
                message.append("\n");
            }
            message
                    .append(i++).append(") ").append(getFileName(fileQueueItem)).append(" ")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_TARGET_FORMAT, new Object[]{fileQueueItem.getTargetFormat().name()}, locale))
                    .append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_STATUS, new Object[]{getStatus(fileQueueItem, locale)}, locale));
        }

        return message.toString();
    }

    public String queryItemNotFound(Locale locale) {
        return localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, locale);
    }

    public String getItem(ConversionQueueItem fileQueueItem, Locale locale) {
        StringBuilder message = new StringBuilder();
        message
                .append(getFileName(fileQueueItem)).append(" ")
                .append(localisationService.getMessage(MessagesProperties.MESSAGE_TARGET_FORMAT, new Object[]{fileQueueItem.getTargetFormat().name()}, locale))
                .append("\n")
                .append(localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_STATUS, new Object[]{getStatus(fileQueueItem, locale)}, locale));

        if (!NON_DISPLAY_FORMATS.contains(fileQueueItem.getFormat())) {
            message.append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_FORMAT, new Object[]{fileQueueItem.getFormat()}, locale));
        }
        if (!NON_SIZEABLE_FORMATS.contains(fileQueueItem.getFormat())) {
            message.append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_SIZE, new Object[]{MemoryUtils.humanReadableByteCount(fileQueueItem.getSize())}, locale));
        }

        return message.toString();
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

    private String getFileName(ConversionQueueItem fileQueueItem) {
        switch (fileQueueItem.getFormat()) {
            case TEXT:
            case URL:
                if (fileQueueItem.getFileId().length() > 11) {
                    return fileQueueItem.getFileId().substring(0, 11) + "...";
                } else {
                    return fileQueueItem.getFileId();
                }
            default:
                return fileQueueItem.getFileName();
        }
    }

    private String getStatus(ConversionQueueItem fileQueueItem, Locale locale) {
        switch (fileQueueItem.getStatus()) {
            case WAITING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_WAITING, new Object[]{fileQueueItem.getPlaceInQueue()}, locale);
            case PROCESSING:
                Duration between = Duration.between(fileQueueItem.getLastRunAt(), timeCreator.now());
                String time = time2TextService.time(between, locale);
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_PROCESSING, new Object[]{time}, locale);
            case EXCEPTION:
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_EXCEPTION, locale);
        }

        return "";
    }
}

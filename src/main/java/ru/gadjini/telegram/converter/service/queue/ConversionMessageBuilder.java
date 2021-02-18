package ru.gadjini.telegram.converter.service.queue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.update.UpdateQueryStatusCommandMessageProvider;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

@Service
public class ConversionMessageBuilder implements UpdateQueryStatusCommandMessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionMessageBuilder.class);

    private static final Set<Format> NON_DISPLAY_FORMATS = Set.of(Format.TEXT);

    private static final Set<Format> NON_SIZEABLE_FORMATS = Set.of(Format.TEXT, Format.URL);

    private LocalisationService localisationService;

    @Value("${converter:all}")
    private String converter;

    @Autowired
    public ConversionMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @PostConstruct
    public void init() {
        LOGGER.debug("Message builder converter({})", converter);
    }

    public String getCompressionInfoMessage(long sourceSize, long resultSize, Locale locale) {
        return localisationService.getMessage(MessagesProperties.MESSAGE_COMPRESSED_SIZE,
                new Object[]{MemoryUtils.humanReadableByteCount(sourceSize), MemoryUtils.humanReadableByteCount(resultSize)}, locale);
    }

    public String getUnsupportedCategoryMessage(FormatCategory category, Locale locale) {
        String msgCode = MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT;
        if (category == FormatCategory.VIDEO) {
            msgCode = MessagesProperties.MESSAGE_VIDEO_CONVERSION;
        } else if (Set.of(FormatCategory.DOCUMENTS, FormatCategory.IMAGES, FormatCategory.WEB).contains(category)) {
            msgCode = MessagesProperties.MESSAGE_DEFAULT_CONVERSION;
        } else if (category == FormatCategory.AUDIO) {
            msgCode = MessagesProperties.MESSAGE_AUDIO_CONVERSION;
        }

        return localisationService.getMessage(msgCode, locale);
    }

    public String getWelcomeMessage(Locale locale) {
        if (FormatsConfiguration.ALL_CONVERTER.equals(converter)) {
            return localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale);
        } else if (FormatsConfiguration.DOCUMENT_CONVERTER.equals(converter)) {
            return localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_DOCUMENT_FILE, locale);
        } else if (FormatsConfiguration.AUDIO_CONVERTER.equals(converter)) {
            return localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_AUDIO_FILE, locale);
        } else {
            return localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_VIDEO_FILE, locale);
        }
    }

    public String getChooseFormat(Locale locale) {
        StringBuilder message = new StringBuilder();
        if (FormatsConfiguration.DOCUMENT_CONVERTER.equals(converter) || FormatsConfiguration.ALL_CONVERTER.equals(converter)) {
            message.append(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION_DEFAULT_CONVERSION, locale));
        } else {
            message.append(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION_VIDEO_AUDIO_CONVERSION, locale));
        }

        return message.toString();
    }

    public String getFilesDownloadingProgressMessage(ConversionQueueItem queueItem, int current, int total, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, locale);

        return progressingMessage + "\n\n" + getFilesDownloadingProgressMessage(current, total, queueItem.getTargetFormat(), locale);
    }

    @Override
    public String getUpdateStatusMessage(QueueItem queueItem, Locale locale) {
        return getConversionProcessingMessage((ConversionQueueItem) queueItem, ConversionStep.WAITING, Collections.emptySet(), locale);
    }

    public String getConversionProcessingMessage(ConversionQueueItem queueItem,
                                                 ConversionStep conversionStep, Set<ConversionStep> completedSteps, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, locale);

        return progressingMessage + "\n\n" +
                getProgressMessage(conversionStep, completedSteps, queueItem.getFirstFileFormat(), queueItem.getTargetFormat(), locale);
    }

    private String getConversionProgressingMessage(ConversionQueueItem queueItem, Locale locale) {
        StringBuilder text = new StringBuilder();
        if (queueItem.getTargetFormat() == Format.COMPRESS) {
            text.append(localisationService.getMessage(MessagesProperties.MESSAGE_COMPRESS_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.MERGE_PDFS) {
            text.append(localisationService.getMessage(MessagesProperties.MESSAGE_MERGE_FILES_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.EDIT_VIDEO) {
            text.append(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_WILL_BE_EDITED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else {
            String queuedMessageCode = queueItem.getFiles().size() > 1 ? MessagesProperties.MESSAGE_FILES_QUEUED : MessagesProperties.MESSAGE_FILE_QUEUED;
            text.append(localisationService.getMessage(queuedMessageCode, new Object[]{queueItem.getTargetFormat().getName(), queueItem.getQueuePosition()}, locale));
        }

        if (!NON_DISPLAY_FORMATS.contains(queueItem.getFirstFileFormat())) {
            text.append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_FORMAT, new Object[]{queueItem.getFirstFileFormat().name()}, locale));
        }
        if (!NON_SIZEABLE_FORMATS.contains(queueItem.getFirstFileFormat())) {
            text.append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_SIZE, new Object[]{MemoryUtils.humanReadableByteCount(queueItem.getFiles().stream()
                            .map(TgFile::getSize).mapToLong(l -> l).sum())}, locale));
        }
        if (queueItem.getFiles().size() > 1) {
            text.append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_FILES_COUNT, new Object[]{queueItem.getFiles().size()}, locale));
        }

        String w = warns(Set.of(localisationService.getMessage(MessagesProperties.MESSAGE_DONT_SEND_NEW_REQUEST, locale)), locale);

        if (StringUtils.isNotBlank(w)) {
            text.append("\n\n").append(w);
        }

        return text.toString();
    }

    private String getProgressMessage(ConversionStep conversionStep, Set<ConversionStep> completedSteps, Format srcFormat, Format targetFormat, Locale locale) {
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);
        String conversionMsgCode = targetFormat == Format.COMPRESS
                ? MessagesProperties.COMPRESSING_STEP
                : targetFormat == Format.MERGE_PDFS ? MessagesProperties.MERGING_STEP
                : targetFormat == Format.EDIT_VIDEO ? MessagesProperties.VIDEO_EDITING_STEP
                : MessagesProperties.CONVERTING_STEP;

        switch (conversionStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(MessagesProperties.WAITING_STEP, locale) + " ...</b>\n" +
                        (srcFormat.isDownloadable()
                                ? "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b>"
                                + (completedSteps.contains(ConversionStep.DOWNLOADING) ? " " + iconCheck : "") + "\n"
                                : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b>" + (completedSteps.contains(ConversionStep.CONVERTING) ? " " + iconCheck : "") + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case DOWNLOADING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + " ...</b>\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case CONVERTING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + " ...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + " ...</b>";
            default:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
    }

    private String getFilesDownloadingProgressMessage(int current, int total, Format targetFormat, Locale locale) {
        return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_FILES_STEP, new Object[]{current, total}, locale) + " ...</b>\n" +
                "<b>" + localisationService.getMessage(targetFormat == Format.COMPRESS ? MessagesProperties.COMPRESSING_STEP : MessagesProperties.CONVERTING_STEP, locale) + "</b>\n" +
                "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
    }

    private String warns(Set<String> warns, Locale locale) {
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

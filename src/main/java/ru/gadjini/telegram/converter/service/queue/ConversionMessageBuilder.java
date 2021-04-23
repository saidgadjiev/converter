package ru.gadjini.telegram.converter.service.queue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.service.conversion.impl.FFmpegAudioCompressConverter;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.queue.DownloadQueueService;
import ru.gadjini.telegram.smart.bot.commons.service.update.UpdateQueryStatusCommandMessageProvider;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class ConversionMessageBuilder implements UpdateQueryStatusCommandMessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionMessageBuilder.class);

    private static final Set<Format> NON_DISPLAY_FORMATS = Set.of(Format.TEXT);

    private static final Set<Format> NON_SIZEABLE_FORMATS = Set.of(Format.TEXT, Format.URL);

    private LocalisationService localisationService;

    private DownloadQueueService downloadQueueService;

    private ApplicationProperties applicationProperties;

    @Autowired
    public ConversionMessageBuilder(LocalisationService localisationService,
                                    DownloadQueueService downloadQueueService,
                                    ApplicationProperties applicationProperties) {
        this.localisationService = localisationService;
        this.downloadQueueService = downloadQueueService;
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void init() {
        LOGGER.debug("Message builder converter({})", applicationProperties.getConverter());
    }

    public String getCompressionInfoMessage(long sourceSize, long resultSize, Locale locale) {
        return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESSED_SIZE,
                new Object[]{MemoryUtils.humanReadableByteCount(sourceSize), MemoryUtils.humanReadableByteCount(resultSize)}, locale);
    }

    public String getResolutionChangedInfoMessage(Integer sourceHeight, Integer targetHeight, Locale locale) {
        if (sourceHeight == null || targetHeight == null) {
            return null;
        }

        return localisationService.getMessage(ConverterMessagesProperties.VIDEO_EDITING_RESOLUTION_CHANGED,
                new Object[]{sourceHeight + "p", targetHeight + "p"}, locale);
    }

    public String getUnsupportedCategoryMessage(FormatCategory category, Locale locale) {
        String msgCode = ConverterMessagesProperties.MESSAGE_UNSUPPORTED_FORMAT;
        if (category == FormatCategory.VIDEO) {
            msgCode = ConverterMessagesProperties.MESSAGE_VIDEO_CONVERSION;
        } else if (Set.of(FormatCategory.DOCUMENTS, FormatCategory.IMAGES, FormatCategory.WEB).contains(category)) {
            msgCode = ConverterMessagesProperties.MESSAGE_DEFAULT_CONVERSION;
        } else if (category == FormatCategory.AUDIO) {
            msgCode = ConverterMessagesProperties.MESSAGE_AUDIO_CONVERSION;
        }

        return localisationService.getMessage(msgCode, locale);
    }

    public String getWelcomeMessage(Locale locale) {
        if (FormatsConfiguration.ALL_CONVERTER.equals(applicationProperties.getConverter())) {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONVERT_FILE, locale);
        } else if (FormatsConfiguration.DOCUMENT_CONVERTER.equals(applicationProperties.getConverter())) {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONVERT_DOCUMENT_FILE, locale);
        } else if (FormatsConfiguration.AUDIO_CONVERTER.equals(applicationProperties.getConverter())) {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONVERT_AUDIO_FILE, locale);
        } else {
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONVERT_VIDEO_FILE, locale);
        }
    }

    public String getChooseFormat(Locale locale) {
        StringBuilder message = new StringBuilder();
        if (applicationProperties.is(FormatsConfiguration.DOCUMENT_CONVERTER)) {
            message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION_DEFAULT_CONVERSION, locale));
        } else {
            message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION_VIDEO_AUDIO_CONVERSION, locale));
            if (FormatsConfiguration.AUDIO_CONVERTER.equals(applicationProperties.getConverter())) {
                message
                        .append("\n\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DEFAULT_COMPRESSION_SETTINGS,
                                new Object[]{FFmpegAudioCompressConverter.AUTO_BITRATE + "k",
                                        FFmpegAudioCompressConverter.DEFAULT_AUDIO_COMPRESS_FORMAT.getName()}, locale))
                        .append("\n\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESS_PROMPT_BITRATE, locale));
            }
        }

        return message.toString();
    }

    public String getFilesDownloadingProgressMessage(ConversionQueueItem queueItem, int current, int total, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, locale);

        return progressingMessage + "\n\n" + getFilesDownloadingProgressMessage(current, total, queueItem.getTargetFormat(), locale);
    }

    @Override
    public String getUpdateStatusMessage(QueueItem queueItem, Locale locale) {
        ConversionStep conversionStep = ConversionStep.WAITING;
        Set<ConversionStep> completedSteps = new HashSet<>();
        if (QueueItem.Status.PROCESSING.equals(queueItem.getStatus())) {
            conversionStep = ConversionStep.CONVERTING;
        } else if (QueueItem.Status.COMPLETED.equals(queueItem.getStatus())) {
            completedSteps.add(ConversionStep.DOWNLOADING);
            completedSteps.add(ConversionStep.CONVERTING);
        } else {
            long downloadedCount = downloadQueueService.getDownloadedFilesCount(applicationProperties.getConverter(), queueItem.getId());

            if (downloadedCount == ((ConversionQueueItem) queueItem).getTotalFilesToDownload()) {
                completedSteps.add(ConversionStep.DOWNLOADING);
            }
        }

        return getConversionProcessingMessage((ConversionQueueItem) queueItem, conversionStep, completedSteps, locale);
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
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESS_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.MERGE_PDFS) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MERGE_FILES_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.EDIT) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_WILL_BE_EDITED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else {
            String queuedMessageCode = queueItem.getFiles().size() > 1 ? ConverterMessagesProperties.MESSAGE_FILES_QUEUED : ConverterMessagesProperties.MESSAGE_FILE_QUEUED;
            text.append(localisationService.getMessage(queuedMessageCode, new Object[]{queueItem.getTargetFormat().getName(), queueItem.getQueuePosition()}, locale));
        }

        if (!NON_DISPLAY_FORMATS.contains(queueItem.getFirstFileFormat())) {
            text.append("\n")
                    .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILE_FORMAT, new Object[]{queueItem.getFirstFileFormat().name()}, locale));
        }
        if (!NON_SIZEABLE_FORMATS.contains(queueItem.getFirstFileFormat())) {
            text.append("\n")
                    .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILE_SIZE, new Object[]{MemoryUtils.humanReadableByteCount(queueItem.getFiles().stream()
                            .map(TgFile::getSize).mapToLong(l -> l).sum())}, locale));
        }
        if (queueItem.getFiles().size() > 1) {
            text.append("\n")
                    .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILES_COUNT, new Object[]{queueItem.getFiles().size()}, locale));
        }

        String w = warns(Set.of(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_SEND_NEW_REQUEST, locale)), locale);

        if (StringUtils.isNotBlank(w)) {
            text.append("\n\n").append(w);
        }

        return text.toString();
    }

    private String getProgressMessage(ConversionStep conversionStep, Set<ConversionStep> completedSteps, Format srcFormat, Format targetFormat, Locale locale) {
        String iconCheck = localisationService.getMessage(ConverterMessagesProperties.ICON_CHECK, locale);
        String conversionMsgCode = targetFormat == Format.COMPRESS
                ? ConverterMessagesProperties.COMPRESSING_STEP
                : targetFormat == Format.MERGE_PDFS ? ConverterMessagesProperties.MERGING_STEP
                : targetFormat == Format.EDIT ? ConverterMessagesProperties.VIDEO_EDITING_STEP
                : ConverterMessagesProperties.CONVERTING_STEP;

        switch (conversionStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(ConverterMessagesProperties.WAITING_STEP, locale) + " ...</b>\n" +
                        (srcFormat.isDownloadable()
                                ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + "</b>"
                                + (completedSteps.contains(ConversionStep.DOWNLOADING) ? " " + iconCheck : "") + "\n"
                                : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b>" + (completedSteps.contains(ConversionStep.CONVERTING) ? " " + iconCheck : "") + "\n" +
                        "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case DOWNLOADING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + " ...</b>\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case CONVERTING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + " ...</b>\n" +
                        "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + " ...</b>";
            default:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n"
                        : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
    }

    private String getFilesDownloadingProgressMessage(int current, int total, Format targetFormat, Locale locale) {
        return "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_FILES_STEP, new Object[]{current, total}, locale) + " ...</b>\n" +
                "<b>" + localisationService.getMessage(targetFormat == Format.COMPRESS ? ConverterMessagesProperties.COMPRESSING_STEP : ConverterMessagesProperties.CONVERTING_STEP, locale) + "</b>\n" +
                "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b>";
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
            return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONVERT_WARNINGS, new Object[]{warnsText.toString()}, locale);
        }

        return null;
    }
}

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
import ru.gadjini.telegram.converter.service.conversion.impl.VaiMakeConverter;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
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
import java.util.LinkedHashSet;
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

    public String getVideoCompressionInfoMessage(long sourceSize, long resultSize,
                                                 int sourceHeight, int targetHeight, Locale locale) {
        return localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_COMPRESSION_CAPTION, locale) + "\n\n"
                + getVideoEditedInfoMessage(sourceSize, resultSize, sourceHeight, targetHeight, locale);
    }

    public String getVideoEditedInfoMessage(long sourceFileSize, long resultSize,
                                            int sourceHeight, int targetHeight, Locale locale) {
        return localisationService.getMessage(ConverterMessagesProperties.VIDEO_EDITING_RESOLUTION_CHANGED,
                new Object[]{sourceHeight + "p", targetHeight + "p"}, locale) + "\n\n"
                + localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESSED_SIZE,
                new Object[]{MemoryUtils.humanReadableByteCount(sourceFileSize), MemoryUtils.humanReadableByteCount(resultSize)}, locale);
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
            return localisationService.getCommandWelcomeMessage(CommandNames.START_COMMAND_NAME,
                    ConverterMessagesProperties.MESSAGE_CONVERT_FILE, locale);
        } else if (FormatsConfiguration.DOCUMENT_CONVERTER.equals(applicationProperties.getConverter())) {
            return localisationService.getCommandWelcomeMessage(CommandNames.START_COMMAND_NAME,
                    ConverterMessagesProperties.MESSAGE_CONVERT_DOCUMENT_FILE, locale);
        } else if (FormatsConfiguration.AUDIO_CONVERTER.equals(applicationProperties.getConverter())) {
            return localisationService.getCommandWelcomeMessage(CommandNames.START_COMMAND_NAME,
                    ConverterMessagesProperties.MESSAGE_CONVERT_AUDIO_FILE, locale);
        } else {
            return localisationService.getCommandWelcomeMessage(CommandNames.START_COMMAND_NAME,
                    ConverterMessagesProperties.MESSAGE_CONVERT_VIDEO_FILE, locale);
        }
    }

    public String getChooseFormat(Format format, Long size, Locale locale) {
        StringBuilder message = new StringBuilder();

        StringBuilder formatSizeInfo = new StringBuilder();
        if (format != null) {
            formatSizeInfo.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILE_FORMAT, new Object[]{format.getName()}, locale));
        }
        if (size != null) {
            if (formatSizeInfo.length() > 0) {
                formatSizeInfo.append(", ");
            }
            formatSizeInfo.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_FILE_SIZE,
                    new Object[]{MemoryUtils.humanReadableByteCount(size)}, locale));
        }
        if (applicationProperties.is(FormatsConfiguration.DOCUMENT_CONVERTER)) {
            message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION_DEFAULT_CONVERSION, locale));

            if (formatSizeInfo.length() > 0) {
                message.append(" ").append(formatSizeInfo.toString());
            }
        } else {
            message.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION_VIDEO_AUDIO_CONVERSION, locale));

            if (formatSizeInfo.length() > 0) {
                message.append(" ").append(formatSizeInfo.toString());
            }
            if (FormatsConfiguration.AUDIO_CONVERTER.equals(applicationProperties.getConverter())) {
                message
                        .append("\n\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE, locale)).append("\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_NON_COMMAND_FEATURES, locale))
                        .append("\n\n\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DEFAULT_COMPRESSION_SETTINGS,
                                new Object[]{FFmpegAudioCompressConverter.AUTO_BITRATE + "k",
                                        FFmpegAudioCompressConverter.DEFAULT_AUDIO_COMPRESS_FORMAT.getName()
                                }, locale))
                        .append("\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_COMPRESS_PROMPT_BITRATE, locale));
            } else {
                message.append("\n\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE, locale)).append("\n")
                        .append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_NON_COMMAND_FEATURES, locale));
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

        return getConversionProcessingMessage((ConversionQueueItem) queueItem, conversionStep, completedSteps, true, locale);
    }

    public String getConversionProcessingMessage(ConversionQueueItem queueItem,
                                                 ConversionStep conversionStep, Set<ConversionStep> completedSteps, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, locale);

        return progressingMessage + "\n\n" +
                getProgressMessage(conversionStep, completedSteps, false, false,
                        queueItem.getFirstFileFormat(), queueItem.getTargetFormat(), locale);
    }

    public String getConversionProcessingMessage(ConversionQueueItem queueItem,
                                                 ConversionStep conversionStep, Set<ConversionStep> completedSteps,
                                                 boolean withConversionPercentage, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, locale);

        return progressingMessage + "\n\n" +
                getProgressMessage(conversionStep, completedSteps, withConversionPercentage, false,
                        queueItem.getFirstFileFormat(), queueItem.getTargetFormat(), locale);
    }

    public String getConversionProcessingMessage(ConversionQueueItem queueItem,
                                                 ConversionStep conversionStep, Set<ConversionStep> completedSteps,
                                                 boolean withPercentage, boolean withEtaSpeedPercentage, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem,
                locale);

        return progressingMessage + "\n\n" +
                getProgressMessage(conversionStep, completedSteps, withPercentage, withEtaSpeedPercentage,
                        queueItem.getFirstFileFormat(), queueItem.getTargetFormat(), locale);
    }

    private String getConversionProgressingMessage(ConversionQueueItem queueItem, Locale locale) {
        StringBuilder text = new StringBuilder();
        if (queueItem.getTargetFormat() == Format.COMPRESS) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_COMPRESS_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.MERGE_PDFS || queueItem.getTargetFormat() == Format.MERGE) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONCATENATE_FILES_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.EDIT) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_WILL_BE_EDITED, new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.MUTE) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_MUTE_QUEUED,
                    new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.PROBE) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_PROBE_QUEUED,
                    new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.UPLOAD) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_UPLOAD_QUEUED,
                    new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.CUT) {
            text.append(localisationService.getMessage(queueItem.getFirstFileFormat().getCategory() == FormatCategory.VIDEO
                            ? ConverterMessagesProperties.MESSAGE_VIDEO_CUT_QUEUED
                            : ConverterMessagesProperties.MESSAGE_AUDIO_CUT_QUEUED,
                    new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.VMAKE) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VMAKE_QUEUED,
                    new Object[]{VaiMakeConverter.OUTPUT_FORMAT.getName(), queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.PREPARE_VIDEO_EDITING) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_PREPARE_VIDEO_EDITING_QUEUED,
                    new Object[]{queueItem.getQueuePosition()}, locale));
        } else if (queueItem.getTargetFormat() == Format.WATERMARK) {
            text.append(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_WATERMARK_QUEUED,
                    new Object[]{queueItem.getQueuePosition()}, locale));
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

        Set<String> warns = new LinkedHashSet<>();
        warns.add(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_SEND_NEW_REQUEST, locale));
        String w = warns(warns, locale);

        if (StringUtils.isNotBlank(w)) {
            text.append("\n\n").append(w);
        }

        return text.toString();
    }

    private String percentageEscape(ConversionStep conversionStep, boolean withPercentage, boolean withEtaStart) {
        return conversionStep.equals(ConversionStep.CONVERTING) ? withPercentage || withEtaStart ? "%%" : "%" : "%";
    }

    private String getProgressMessage(ConversionStep conversionStep, Set<ConversionStep> completedSteps,
                                      boolean withPercentage, boolean withEtaStart,
                                      Format srcFormat, Format targetFormat, Locale locale) {
        String iconCheck = localisationService.getMessage(ConverterMessagesProperties.ICON_CHECK, locale);
        String conversionMsgCode = targetFormat == Format.COMPRESS
                ? ConverterMessagesProperties.COMPRESSING_STEP
                : targetFormat == Format.MERGE_PDFS ? ConverterMessagesProperties.CONCATENATION_STEP
                : targetFormat == Format.EDIT ? ConverterMessagesProperties.VIDEO_EDITING_STEP
                : targetFormat == Format.VMAKE ? ConverterMessagesProperties.VIDEO_MAKING_STEP
                : targetFormat == Format.MUTE ? ConverterMessagesProperties.MUTING_STEP
                : targetFormat == Format.CUT ? ConverterMessagesProperties.CUTTING_STEP
                : targetFormat == Format.PROBE ? ConverterMessagesProperties.PROBING_STEP
                : targetFormat == Format.WATERMARK ? ConverterMessagesProperties.WATERMARK_ADDING_STEP
                : ConverterMessagesProperties.CONVERTING_STEP;

        String percentageEscape = percentageEscape(conversionStep, withPercentage, withEtaStart);
        String progressPart = withPercentage ? " (%s%%)</b>\n" : " ...</b>\n";
        String percentageCompleted = withPercentage ? " <b>(100%)</b>" + iconCheck : iconCheck;
        String eta = withEtaStart ? "<pre>" + localisationService.getMessage(ConverterMessagesProperties.MESSAGE_ETA, locale) + " %s</pre>\n" +
                "<pre>" + localisationService.getMessage(ConverterMessagesProperties.MESSAGE_SPEED, locale) + " %s</pre>\n" : "";

        switch (conversionStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(ConverterMessagesProperties.WAITING_STEP, locale) + " ...</b>\n" +
                        (srcFormat.isDownloadable()
                                ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + "</b>"
                                + (completedSteps.contains(ConversionStep.DOWNLOADING) ? " <b>(100%)</b>" + iconCheck : "") + "\n"
                                : "") +
                        (targetFormat == Format.UPLOAD ? "" : "<b>" + localisationService.getMessage(conversionMsgCode, locale)
                                + "</b>" + (completedSteps.contains(ConversionStep.CONVERTING) ? percentageCompleted : "") + "\n") +
                        (targetFormat.isUploadable() ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b>" : "");
            case DOWNLOADING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + " ...</b>\n"
                        : "") +
                        (targetFormat == Format.UPLOAD ? "" : "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b>\n") +
                        (targetFormat.isUploadable() ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b>" : "");
            case CONVERTING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale)
                        + " (100" + percentageEscape + ")</b>" + iconCheck + "\n"
                        : "") +
                        (targetFormat == Format.UPLOAD ? "" : "<b>" + localisationService.getMessage(conversionMsgCode, locale) + progressPart) +
                        eta +
                        (targetFormat.isUploadable() ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + "</b>" : "");
            case UPLOADING:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + " (100%)</b>" + iconCheck + "\n"
                        : "") +
                        (targetFormat == Format.UPLOAD ? "" : "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + percentageCompleted + "\n") +
                        (targetFormat.isUploadable() ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + " ...</b>" : "");
            default:
                return (srcFormat.isDownloadable()
                        ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.DOWNLOADING_STEP, locale) + " (100%)</b>" + iconCheck + "\n"
                        : "") +
                        (targetFormat == Format.UPLOAD ? "" : "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + percentageCompleted + "\n") +
                        (targetFormat.isUploadable() ? "<b>" + localisationService.getMessage(ConverterMessagesProperties.UPLOADING_STEP, locale) + " (100%)</b>" + iconCheck : "");
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

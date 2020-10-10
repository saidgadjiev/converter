package ru.gadjini.telegram.converter.service.queue;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.ProgressManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

@Service
public class ConversionMessageBuilder {

    private static final Set<Format> NON_DISPLAY_FORMATS = Set.of(Format.TEXT);

    private static final Set<Format> NON_SIZEABLE_FORMATS = Set.of(Format.TEXT, Format.URL);

    private LocalisationService localisationService;

    private ProgressManager progressManager;

    @Autowired
    public ConversionMessageBuilder(LocalisationService localisationService, ProgressManager progressManager) {
        this.localisationService = localisationService;
        this.progressManager = progressManager;
    }

    public String getFilesDownloadingProgressMessage(long fileSize, int current, int total, Format targetFormat, Lang lang, Locale locale) {
        String formatter = lang == Lang.JAVA ? "%s" : "{}";
        String percentage = lang == Lang.JAVA ? "%%" : "%";
        boolean progress = isShowingProgress(fileSize, ConversionStep.DOWNLOADING);
        String percentageFormatter = progress ? "(" + formatter + percentage + ")..." : "...";

        return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_FILES_STEP, new Object[]{current, total}, locale) + " " + percentageFormatter + "</b>\n" +
                (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>\n" : "") +
                "<b>" + localisationService.getMessage(targetFormat == Format.COMPRESS ? MessagesProperties.COMPRESSING_STEP : MessagesProperties.CONVERTING_STEP, locale) + "</b>\n" +
                "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
    }

    public String getProgressMessage(long fileSize, ConversionStep conversionStep, Format targetFormat, Lang lang, Locale locale) {
        String formatter = lang == Lang.JAVA ? "%s" : "{}";
        String percentage = lang == Lang.JAVA ? "%%" : "%";
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);
        boolean progress = isShowingProgress(fileSize, conversionStep);
        String percentageFormatter = progress ? "(" + formatter + percentage + ")..." : "...";
        String conversionMsgCode = targetFormat == Format.COMPRESS ? MessagesProperties.COMPRESSING_STEP : MessagesProperties.CONVERTING_STEP;

        switch (conversionStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(MessagesProperties.WAITING_STEP, locale) + "...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case DOWNLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + " " + percentageFormatter + "</b>\n" +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>\n" : "") +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case CONVERTING:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + " ...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + " " + percentageFormatter + "</b>\n" +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>" : "");
            default:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(conversionMsgCode, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
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

    public String getConversionProgressingMessage(ConversionQueueItem queueItem, Set<String> warns, Locale locale) {
        StringBuilder text = new StringBuilder();
        if (queueItem.getTargetFormat() == Format.COMPRESS) {
            text.append(localisationService.getMessage(MessagesProperties.MESSAGE_VIDEO_COMPRESS_QUEUED, new Object[]{queueItem.getPlaceInQueue()}, locale));
        } else {
            String queuedMessageCode = queueItem.getFiles().size() > 1 ? MessagesProperties.MESSAGE_FILES_QUEUED : MessagesProperties.MESSAGE_FILE_QUEUED;
            text.append(localisationService.getMessage(queuedMessageCode, new Object[]{queueItem.getTargetFormat().getName(), queueItem.getPlaceInQueue()}, locale));
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

        String w = warns(warns, locale);

        if (StringUtils.isNotBlank(w)) {
            text.append("\n\n").append(w);
        }

        return text.toString();
    }

    public String getFilesDownloadingProgressMessage(ConversionQueueItem queueItem, long fileSize, int current, int total, Lang lang, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, Collections.emptySet(), locale);

        return progressingMessage + "\n\n" + getFilesDownloadingProgressMessage(fileSize, current, total, queueItem.getTargetFormat(), lang, locale);
    }

    public String getConversionProcessingMessage(ConversionQueueItem queueItem, long fileSize, Set<String> warns,
                                                 ConversionStep conversionStep, Lang lang, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, warns, locale);

        return progressingMessage + "\n\n" + getProgressMessage(fileSize, conversionStep, queueItem.getTargetFormat(), lang, locale);
    }

    public String getUploadingProgressMessage(ConversionQueueItem queueItem, Locale locale) {
        String progressingMessage = getConversionProgressingMessage(queueItem, Collections.emptySet(), locale);
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);

        return progressingMessage + "\n\n" +
                "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                "<b>" + localisationService.getMessage(queueItem.getTargetFormat() == Format.COMPRESS ? MessagesProperties.COMPRESSING_STEP : MessagesProperties.CONVERTING_STEP, locale) + "</b> " + iconCheck + "\n" +
                "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "...</b>\n";
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

    private boolean isShowingProgress(long fileSize, ConversionStep conversionStep) {
        if (conversionStep == ConversionStep.DOWNLOADING) {
            return progressManager.isShowingDownloadingProgress(fileSize);
        } else if (conversionStep == ConversionStep.UPLOADING) {
            return progressManager.isShowingUploadingProgress(fileSize);
        }

        return false;
    }
}

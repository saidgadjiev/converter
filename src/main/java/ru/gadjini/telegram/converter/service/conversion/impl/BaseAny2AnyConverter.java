package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;

import java.util.*;

public abstract class BaseAny2AnyConverter implements Any2AnyConverter {

    private final Map<List<Format>, List<Format>> map;

    private ConversionMessageBuilder messageBuilder;

    private SmartInlineKeyboardService inlineKeyboardService;

    private UserService userService;

    private TempFileService fileService;

    private FileDownloadService fileDownloadService;

    protected BaseAny2AnyConverter(Map<List<Format>, List<Format>> map) {
        this.map = map;
    }

    @Autowired
    public void setFileDownloadService(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setMessageBuilder(ConversionMessageBuilder messageBuilder) {
        this.messageBuilder = messageBuilder;
    }

    @Autowired
    public void setInlineKeyboardService(SmartInlineKeyboardService inlineKeyboardService) {
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Autowired
    public void setFileService(TempFileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public final boolean accept(Format format, Format targetFormat) {
        return isConvertAvailable(format, targetFormat);
    }

    TempFileService getFileService() {
        return fileService;
    }

    @Override
    public void createDownloads(ConversionQueueItem conversionQueueItem) {
        conversionQueueItem.getFirstFile().setProgress(progress(conversionQueueItem.getUserId(), conversionQueueItem));
        fileDownloadService.createDownload(conversionQueueItem.getFirstFile(), conversionQueueItem.getId(), conversionQueueItem.getUserId());
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        return doConvert(fileQueueItem);
    }

    final SmartTempFile downloadThumb(ConversionQueueItem fileQueueItem) {
        if (StringUtils.isNotBlank(fileQueueItem.getFirstFile().getThumb())) {
            return fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFile().getThumb());
        } else {
            return null;
        }
    }

    private Progress progress(long chatId, ConversionQueueItem queueItem) {
        Progress progress = new Progress();
        progress.setChatId(chatId);

        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        progress.setProgressMessageId(queueItem.getProgressMessageId());
        String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(), ConversionStep.DOWNLOADING, Collections.emptySet(), locale);
        progress.setProgressMessage(progressMessage);
        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(),
                ConversionStep.WAITING, Set.of(ConversionStep.DOWNLOADING), locale);
        progress.setAfterProgressCompletionMessage(completionMessage);
        progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        return progress;
    }

    private boolean isConvertAvailable(Format src, Format target) {
        return getTargetFormats(src).contains(target);
    }

    private List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<List<Format>, List<Format>> entry : map.entrySet()) {
            if (entry.getKey().contains(srcFormat)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }

    protected abstract ConvertResult doConvert(ConversionQueueItem conversionQueueItem);
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseAny2AnyConverter implements Any2AnyConverter {

    private final Map<List<Format>, List<Format>> map;

    private ConversionMessageBuilder messageBuilder;

    private InlineKeyboardService inlineKeyboardService;

    private UserService userService;

    protected BaseAny2AnyConverter(Map<List<Format>, List<Format>> map) {
        this.map = map;
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
    public void setInlineKeyboardService(InlineKeyboardService inlineKeyboardService) {
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public final boolean accept(Format format, Format targetFormat) {
        return isConvertAvailable(format, targetFormat);
    }

    protected final Progress progress(long chatId, ConversionQueueItem queueItem) {
        Progress progress = new Progress();
        progress.setChatId(chatId);

        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        progress.setLocale(locale.getLanguage());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, queueItem.getSize(), Collections.emptySet(), ConversionStep.DOWNLOADING, Lang.PYTHON, locale);
        progress.setProgressMessage(progressMessage);
        progress.setProgressReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));

        String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, queueItem.getSize(), Collections.emptySet(), ConversionStep.CONVERTING, Lang.PYTHON, locale);
        progress.setAfterProgressCompletionMessage(completionMessage);
        progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));

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
}

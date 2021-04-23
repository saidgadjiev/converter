package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartReplyKeyboardService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService.keyboardRow;
import static ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService.replyKeyboardMarkup;

@Service
@Qualifier("keyboard")
public class ReplyKeyboardServiceImpl implements ConverterReplyKeyboardService {

    private ConversionFormatService formatMapService;

    private LocalisationService localisationService;

    private SmartReplyKeyboardService smartReplyKeyboardService;

    @Autowired
    public ReplyKeyboardServiceImpl(@Lazy ConversionFormatService formatMapService,
                                    LocalisationService localisationService, SmartReplyKeyboardService smartReplyKeyboardService) {
        this.formatMapService = formatMapService;
        this.localisationService = localisationService;
        this.smartReplyKeyboardService = smartReplyKeyboardService;
    }

    @Override
    public ReplyKeyboard mainMenuKeyboard(long chatId, Locale locale) {
        return removeKeyboard(chatId);
    }

    @Override
    public ReplyKeyboardMarkup smartFileFeatureKeyboard(long chatId, Locale locale) {
        return smartReplyKeyboardService.smartFileFeatureKeyboard(locale);
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        return smartReplyKeyboardService.languageKeyboard(locale);
    }

    @Override
    public ReplyKeyboardMarkup audioCompressionKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup mergePdfsKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.MERGE_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup formatsKeyboard(long chatId, Format format, Locale locale) {
        List<Format> targetFormats = new ArrayList<>(formatMapService.getTargetFormats(format));
        targetFormats.sort(Comparator.comparing(Format::getName));

        if (targetFormats.remove(Format.COMPRESS)) {
            targetFormats.add(0, Format.COMPRESS);
        }
        if (targetFormats.remove(Format.STREAM)) {
            targetFormats.add(1, Format.STREAM);
        }
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<KeyboardRow> keyboard = replyKeyboardMarkup.getKeyboard();
        List<List<Format>> lists = Lists.partition(targetFormats, 3);
        for (List<Format> list : lists) {
            keyboard.add(keyboardRow(list.stream().map(Format::getName).toArray(String[]::new)));
        }
        keyboard.add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup goBackKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup vmakeKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(
                localisationService.getMessage(ConverterMessagesProperties.VMAKE_COMMAND_NAME, locale),
                localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale))
        );
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        return smartReplyKeyboardService.removeKeyboard();
    }

}

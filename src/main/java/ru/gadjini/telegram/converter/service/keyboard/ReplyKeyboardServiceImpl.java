package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Qualifier("keyboard")
public class ReplyKeyboardServiceImpl implements ConverterReplyKeyboardService {

    private ConversionFormatService formatMapService;

    private LocalisationService localisationService;

    @Autowired
    public ReplyKeyboardServiceImpl(ConversionFormatService formatMapService, LocalisationService localisationService) {
        this.formatMapService = formatMapService;
        this.localisationService = localisationService;
    }

    @Override
    public ReplyKeyboard getMainMenu(long chatId, Locale locale) {
        return removeKeyboard(chatId);
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<String> languages = new ArrayList<>();
        for (Locale l : localisationService.getSupportedLocales()) {
            languages.add(StringUtils.capitalize(l.getDisplayLanguage(l)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(languages.toArray(new String[0])));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup getFormatsKeyboard(long chatId, Format format, Locale locale) {
        List<Format> targetFormats = new ArrayList<>(formatMapService.getTargetFormats(format));
        targetFormats.sort(Comparator.comparing(Format::getName));

        if (targetFormats.remove(Format.COMPRESS)) {
            targetFormats.add(0, Format.COMPRESS);
        }
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<KeyboardRow> keyboard = replyKeyboardMarkup.getKeyboard();
        List<List<Format>> lists = Lists.partition(targetFormats, 3);
        for (List<Format> list : lists) {
            keyboard.add(keyboardRow(list.stream().map(Format::getName).toArray(String[]::new)));
        }
        keyboard.add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        return new ReplyKeyboardRemove();
    }

}

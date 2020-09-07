package ru.gadjini.telegram.converter.service.keyboard;

import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.telegram.smart.bot.commons.service.conversion.api.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public interface ConverterReplyKeyboardService extends ReplyKeyboardService {

    ReplyKeyboardMarkup getFormatsKeyboard(long chatId, Format format, Locale locale);

    ReplyKeyboardRemove removeKeyboard(long chatId);

    default KeyboardRow keyboardRow(String... buttons) {
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.addAll(Arrays.asList(buttons));

        return keyboardRow;
    }

    default ReplyKeyboardMarkup replyKeyboardMarkup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setKeyboard(new ArrayList<>());
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }
}

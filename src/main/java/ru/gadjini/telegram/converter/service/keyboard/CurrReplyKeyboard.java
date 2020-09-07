package ru.gadjini.telegram.converter.service.keyboard;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.dao.command.keyboard.ReplyKeyboardDao;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.service.conversion.api.Format;

import java.util.Locale;

@Service
@Qualifier("curr")
public class CurrReplyKeyboard implements ConverterReplyKeyboardService {

    private ReplyKeyboardDao replyKeyboardDao;

    private ConverterReplyKeyboardService keyboardService;

    public CurrReplyKeyboard(@Qualifier("inMemory") ReplyKeyboardDao replyKeyboardDao, @Qualifier("keyboard") ConverterReplyKeyboardService keyboardService) {
        this.replyKeyboardDao = replyKeyboardDao;
        this.keyboardService = keyboardService;
    }

    @Override
    public ReplyKeyboard getMainMenu(long chatId, Locale locale) {
        return keyboardService.getMainMenu(chatId, locale);
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, (ReplyKeyboardMarkup) keyboardService.languageKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup getFormatsKeyboard(long chatId, Format format, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.getFormatsKeyboard(chatId, format, locale));
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        ReplyKeyboardRemove replyKeyboardRemove = keyboardService.removeKeyboard(chatId);
        setCurrentKeyboard(chatId, new ReplyKeyboardMarkup());

        return replyKeyboardRemove;
    }

    public ReplyKeyboardMarkup getCurrentReplyKeyboard(long chatId) {
        return replyKeyboardDao.get(chatId);
    }

    private ReplyKeyboardMarkup setCurrentKeyboard(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        replyKeyboardDao.store(chatId, replyKeyboardMarkup);

        return replyKeyboardMarkup;
    }
}

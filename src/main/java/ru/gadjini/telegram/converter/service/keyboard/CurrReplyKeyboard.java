package ru.gadjini.telegram.converter.service.keyboard;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.Redis;
import ru.gadjini.telegram.smart.bot.commons.dao.command.keyboard.ReplyKeyboardDao;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardHolderService;

import java.util.Locale;

import static ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService.replyKeyboardMarkup;

@Service
@KeyboardHolder
public class CurrReplyKeyboard implements ConverterReplyKeyboardService, ReplyKeyboardHolderService {

    private ReplyKeyboardDao replyKeyboardDao;

    private ConverterReplyKeyboardService keyboardService;

    public CurrReplyKeyboard(@Redis ReplyKeyboardDao replyKeyboardDao,
                             @Qualifier("keyboard") ConverterReplyKeyboardService keyboardService) {
        this.replyKeyboardDao = replyKeyboardDao;
        this.keyboardService = keyboardService;
    }

    @Override
    public ReplyKeyboard mainMenuKeyboard(long chatId, Locale locale) {
        return removeKeyboard(chatId);
    }

    @Override
    public ReplyKeyboardMarkup smartFileFeatureKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.smartFileFeatureKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.languageKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup audioCompressionKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.audioCompressionKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup mergePdfsKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.mergePdfsKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup formatsKeyboard(long chatId, Format format, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.formatsKeyboard(chatId, format, locale));
    }

    @Override
    public ReplyKeyboardMarkup goBackKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.goBackKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup videoEditKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.videoEditKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup videoCutKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.videoCutKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup vaimakeKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.vaimakeKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup vavmergeKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.vavmergeKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        ReplyKeyboardRemove replyKeyboardRemove = keyboardService.removeKeyboard(chatId);
        setCurrentKeyboard(chatId, replyKeyboardMarkup());

        return replyKeyboardRemove;
    }

    @Override
    public ReplyKeyboardMarkup getCurrentReplyKeyboard(long chatId) {
        return replyKeyboardDao.get(chatId);
    }

    private ReplyKeyboardMarkup setCurrentKeyboard(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        replyKeyboardDao.store(chatId, replyKeyboardMarkup);

        return replyKeyboardMarkup;
    }
}

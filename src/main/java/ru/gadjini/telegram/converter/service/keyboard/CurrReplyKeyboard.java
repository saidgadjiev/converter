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

import java.util.List;
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
    public ReplyKeyboardMarkup mergeFilesKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.mergeFilesKeyboard(chatId, locale));
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
    public ReplyKeyboardMarkup mediaCutKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.mediaCutKeyboard(chatId, locale));
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
    public ReplyKeyboardMarkup watermarkTypeKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkTypeKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup watermarkTextFontSizeKeyboard(long chatId, Locale locale, List<String> fontSizes) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkTextFontSizeKeyboard(chatId, locale, fontSizes));
    }

    @Override
    public ReplyKeyboardMarkup watermarkPositionKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkPositionKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup watermarkTextColorKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkTextColorKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup watermarkImageWidthKeyboard(long chatId, Locale locale, List<String> widths) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkImageWidthKeyboard(chatId, locale, widths));
    }

    @Override
    public ReplyKeyboardMarkup watermarkImageKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkImageKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup watermarkTextKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkTextKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup watermarkOkKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.watermarkOkKeyboard(chatId, locale));
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

package ru.gadjini.telegram.converter.service.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService;

import java.util.Locale;

public interface ConverterReplyKeyboardService extends ReplyKeyboardService {

    ReplyKeyboardMarkup audioCompressionKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup mergeFilesKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup formatsKeyboard(long chatId, Format format, Locale locale);

    ReplyKeyboardMarkup goBackKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup videoEditKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup mediaCutKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup vaimakeKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup vavmergeKeyboard(long chatId, Locale locale);

    ReplyKeyboardRemove removeKeyboard(long chatId);
}

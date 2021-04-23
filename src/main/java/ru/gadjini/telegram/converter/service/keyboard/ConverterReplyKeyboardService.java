package ru.gadjini.telegram.converter.service.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService;

import java.util.Locale;

public interface ConverterReplyKeyboardService extends ReplyKeyboardService {

    ReplyKeyboardMarkup audioCompressionKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup mergePdfsKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup formatsKeyboard(long chatId, Format format, Locale locale);

    ReplyKeyboardMarkup goBackKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup vmakeKeyboard(long chatId, Locale locale);

    ReplyKeyboardRemove removeKeyboard(long chatId);
}

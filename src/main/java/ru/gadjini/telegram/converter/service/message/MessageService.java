package ru.gadjini.telegram.converter.service.message;

import ru.gadjini.telegram.converter.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.converter.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.util.Locale;
import java.util.function.Consumer;

public interface MessageService {

    void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery);

    boolean isChatMember(String chatId, int userId);

    void sendMessage(SendMessage sendMessage);

    void sendMessage(SendMessage sendMessage, Consumer<Message> callback);

    void removeInlineKeyboard(long chatId, int messageId);

    void editMessage(EditMessageText messageContext);

    void editMessageCaption(EditMessageCaption context);

    void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale);

    void deleteMessage(long chatId, int messageId);

    void sendErrorMessage(long chatId, Locale locale);

}

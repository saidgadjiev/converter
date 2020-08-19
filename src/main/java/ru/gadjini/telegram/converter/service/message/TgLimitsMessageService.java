package ru.gadjini.telegram.converter.service.message;

import com.google.common.base.Splitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.converter.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Service
@Qualifier("messagelimits")
public class TgLimitsMessageService implements MessageService {

    public static final int TEXT_LENGTH_LIMIT = 4000;

    private MessageService messageService;

    @Autowired
    public void setMessageService(@Qualifier("asyncmessage") MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        messageService.sendAnswerCallbackQuery(answerCallbackQuery);
    }

    @Override
    public boolean isChatMember(String chatId, int userId) {
        return messageService.isChatMember(chatId, userId);
    }

    @Override
    public void sendMessage(SendMessage sendMessage) {
        messageService.sendMessage(sendMessage);
    }

    @Override
    public void sendMessage(SendMessage sendMessage, Consumer<Message> callback) {
        if (sendMessage.getText().length() < TEXT_LENGTH_LIMIT) {
            messageService.sendMessage(sendMessage, callback);
        } else {
            List<String> parts = new ArrayList<>();
            Splitter.fixedLength(TEXT_LENGTH_LIMIT)
                    .split(sendMessage.getText())
                    .forEach(parts::add);
            for (int i = 0; i < parts.size() - 1; ++i) {
                SendMessage msg = new SendMessage(sendMessage.getChatId(), parts.get(i))
                        .setReplyToMessageId(sendMessage.getReplyToMessageId())
                        .setDisableWebPagePreview(sendMessage.getDisableWebPagePreview())
                        .setParseMode(sendMessage.getParseMode());
                messageService.sendMessage(msg);
            }

            SendMessage msg = new SendMessage(sendMessage.getChatId(), parts.get(parts.size() - 1))
                    .setReplyToMessageId(sendMessage.getReplyToMessageId())
                    .setDisableWebPagePreview(sendMessage.getDisableWebPagePreview())
                    .setParseMode(sendMessage.getParseMode())
                    .setReplyMarkup(sendMessage.getReplyMarkup());
            messageService.sendMessage(msg, callback);
        }
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        messageService.removeInlineKeyboard(chatId, messageId);
    }

    @Override
    public void editMessage(EditMessageText messageContext) {
        messageService.editMessage(messageContext);
    }

    @Override
    public void editMessageCaption(EditMessageCaption context) {
        messageService.editMessageCaption(context);
    }

    @Override
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        messageService.sendBotRestartedMessage(chatId, replyKeyboard, locale);
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        messageService.deleteMessage(chatId, messageId);
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        messageService.sendErrorMessage(chatId, locale);
    }
}

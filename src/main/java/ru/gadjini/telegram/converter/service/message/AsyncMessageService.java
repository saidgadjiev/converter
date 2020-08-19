package ru.gadjini.telegram.converter.service.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.job.TgMethodExecutor;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.converter.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.converter.service.LocalisationService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
@Qualifier("asyncmessage")
public class AsyncMessageService implements MessageService {

    private LocalisationService localisationService;

    private TgMethodExecutor messageSenderJob;

    private MessageService messageService;

    @Autowired
    public AsyncMessageService(LocalisationService localisationService, TgMethodExecutor messageSenderJob,
                               @Qualifier("message") MessageService messageService) {
        this.localisationService = localisationService;
        this.messageSenderJob = messageSenderJob;
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
        sendMessage(sendMessage, null);
    }

    @Override
    public void sendMessage(SendMessage sendMessage, Consumer<Message> callback) {
        messageSenderJob.push(() -> messageService.sendMessage(sendMessage, callback));
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        messageSenderJob.push(() -> messageService.removeInlineKeyboard(chatId, messageId));
    }

    @Override
    public void editMessage(EditMessageText editMessageText) {
        messageSenderJob.push(() -> messageService.editMessage(editMessageText));
    }

    @Override
    public void editMessageCaption(EditMessageCaption editMessageCaption) {
        messageSenderJob.push(() -> messageService.editMessageCaption(editMessageCaption));
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        messageSenderJob.push(() -> messageService.deleteMessage(chatId, messageId));
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        sendMessage(new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ERROR, locale)));
    }

    @Override
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        sendMessage(
                new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_BOT_RESTARTED, locale))
                        .setReplyMarkup(replyKeyboard)
        );
    }
}

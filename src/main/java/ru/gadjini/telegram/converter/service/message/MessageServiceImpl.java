package ru.gadjini.telegram.converter.service.message;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.exception.botapi.TelegramApiException;
import ru.gadjini.telegram.converter.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.converter.model.bot.api.method.IsChatMember;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.DeleteMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageReplyMarkup;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.converter.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.ParseMode;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.telegram.TelegramBotApiService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
@Qualifier("message")
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    private LocalisationService localisationService;

    private TelegramBotApiService telegramService;

    @Autowired
    public MessageServiceImpl(LocalisationService localisationService,
                              TelegramBotApiService telegramService) {
        this.localisationService = localisationService;
        this.telegramService = telegramService;
    }

    @Override
    public void editReplyMarkup(long chatId, int messageId, InlineKeyboardMarkup replyMarkup) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(chatId);
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup(replyMarkup);

        try {
            telegramService.editReplyMarkup(editMessageReplyMarkup);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            telegramService.sendAnswerCallbackQuery(answerCallbackQuery);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isChatMember(String chatId, int userId) {
        IsChatMember isChatMember = new IsChatMember();
        isChatMember.setChatId(chatId);
        isChatMember.setUserId(userId);

        try {
            return BooleanUtils.toBoolean(telegramService.isChatMember(isChatMember));
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void sendMessage(SendMessage sendMessage) {
        sendMessage(sendMessage, null);
    }

    @Override
    public void sendMessage(SendMessage sendMessage, Consumer<Message> callback) {
        sendMessage0(sendMessage, callback);
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);

        try {
            telegramService.editReplyMarkup(edit);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void editMessage(EditMessageText editMessageText) {
        editMessageText.setParseMode(ParseMode.HTML);

        try {
            telegramService.editMessageText(editMessageText);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
            if (editMessageText.isThrowEx()) {
                throw ex;
            }
        }
    }

    @Override
    public void editMessageCaption(EditMessageCaption editMessageCaption) {
        editMessageCaption.setParseMode(ParseMode.HTML);

        try {
            telegramService.editMessageCaption(editMessageCaption);
        } catch (TelegramApiException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        try {
            telegramService.deleteMessage(new DeleteMessage(chatId, messageId));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
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

    private void sendMessage0(SendMessage sendMessage, Consumer<Message> callback) {
        try {
            Message message = telegramService.sendMessage(sendMessage);

            if (callback != null) {
                callback.accept(message);
            }
        } catch (TelegramApiRequestException ex) {
            LOGGER.error("Error send message({}, {}, {}, {})", ex.getChatId(), ex.getErrorCode(), ex.getApiResponse(), sendMessage);
            throw ex;
        }
    }
}

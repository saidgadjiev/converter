package ru.gadjini.telegram.converter.model.bot.api.method.updatemessages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.ParseMode;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

public class EditMessageText {
    public static final String METHOD = "editmessagetext";

    private static final String CHATID_FIELD = "chat_id";
    private static final String MESSAGEID_FIELD = "message_id";
    private static final String TEXT_FIELD = "text";
    private static final String PARSE_MODE_FIELD = "parse_mode";
    private static final String DISABLE_WEB_PREVIEW_FIELD = "disable_web_page_preview";
    private static final String REPLYMARKUP_FIELD = "reply_markup";

    @JsonProperty(CHATID_FIELD)
    private String chatId;
    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId;
    @JsonProperty(TEXT_FIELD)
    private String text;
    @JsonProperty(PARSE_MODE_FIELD)
    private String parseMode;
    @JsonProperty(DISABLE_WEB_PREVIEW_FIELD)
    private Boolean disableWebPagePreview;
    @JsonProperty(REPLYMARKUP_FIELD)
    private InlineKeyboardMarkup replyMarkup;
    @JsonIgnore
    private boolean throwEx;

    public EditMessageText() {
    }

    public EditMessageText(Long chatId, int messageId, String text) {
        this.chatId = chatId.toString();
        this.messageId = messageId;
        this.text = text;
    }

    public EditMessageText(int chatId, int messageId, String text) {
        this((long) chatId, messageId, text);
    }

    public String getChatId() {
        return chatId;
    }

    public EditMessageText setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public EditMessageText setChatId(Long chatId) {
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public EditMessageText setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    public String getText() {
        return text;
    }

    public EditMessageText setText(String text) {
        this.text = text;
        return this;
    }

    public InlineKeyboardMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public EditMessageText setReplyMarkup(InlineKeyboardMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    public EditMessageText disableWebPagePreview() {
        disableWebPagePreview = true;
        return this;
    }

    public EditMessageText enableWebPagePreview() {
        disableWebPagePreview = null;
        return this;
    }

    public EditMessageText enableMarkdown(boolean enable) {
        if (enable) {
            this.parseMode = ParseMode.MARKDOWN;
        } else {
            this.parseMode = null;
        }
        return this;
    }

    public EditMessageText enableHtml(boolean enable) {
        if (enable) {
            this.parseMode = ParseMode.HTML;
        } else {
            this.parseMode = null;
        }
        return this;
    }


    public EditMessageText setParseMode(String parseMode) {
        this.parseMode = parseMode;
        return this;
    }

    public boolean isThrowEx() {
        return throwEx;
    }

    public EditMessageText setThrowEx(boolean throwEx) {
        this.throwEx = throwEx;

        return this;
    }

    @Override
    public String toString() {
        return "EditMessageText{" +
                "chatId=" + chatId +
                ", messageId=" + messageId +
                ", text=" + text +
                ", parseMode=" + parseMode +
                ", disableWebPagePreview=" + disableWebPagePreview +
                ", replyMarkup=" + replyMarkup +
                '}';
    }
}

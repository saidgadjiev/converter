package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

public class Progress {

    private static final String PROGRESS_MESSAGE = "progress_message";

    private static final String AFTER_PROGRESS_COMPLETION_MESSAGE = "after_progress_completion_message";

    private static final String AFTER_PROGRESS_COMPLETION_REPLY_MARKUP = "after_progress_completion_reply_markup";

    private static final String PROGRESS_MESSAGE_ID = "progress_message_id";

    private static final String PROGRESS_REPLY_MARKUP = "progress_reply_markup";

    private static final String CHAT_ID = "chat_id";

    @JsonProperty(CHAT_ID)
    private String chatId;

    @JsonProperty(PROGRESS_MESSAGE)
    private String progressMessage;

    @JsonProperty(AFTER_PROGRESS_COMPLETION_MESSAGE)
    private String afterProgressCompletionMessage;

    @JsonProperty(PROGRESS_MESSAGE_ID)
    private int progressMessageId;

    @JsonProperty(PROGRESS_REPLY_MARKUP)
    private InlineKeyboardMarkup progressReplyMarkup;

    @JsonProperty(AFTER_PROGRESS_COMPLETION_REPLY_MARKUP)
    private InlineKeyboardMarkup afterProgressCompletionReplyMarkup;

    private String locale;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = String.valueOf(chatId);
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public int getProgressMessageId() {
        return progressMessageId;
    }

    public void setProgressMessageId(int progressMessageId) {
        this.progressMessageId = progressMessageId;
    }

    public String getAfterProgressCompletionMessage() {
        return afterProgressCompletionMessage;
    }

    public void setAfterProgressCompletionMessage(String afterProgressCompletionMessage) {
        this.afterProgressCompletionMessage = afterProgressCompletionMessage;
    }

    public InlineKeyboardMarkup getProgressReplyMarkup() {
        return progressReplyMarkup;
    }

    public void setProgressReplyMarkup(InlineKeyboardMarkup progressReplyMarkup) {
        this.progressReplyMarkup = progressReplyMarkup;
    }

    public InlineKeyboardMarkup getAfterProgressCompletionReplyMarkup() {
        return afterProgressCompletionReplyMarkup;
    }

    public void setAfterProgressCompletionReplyMarkup(InlineKeyboardMarkup afterProgressCompletionReplyMarkup) {
        this.afterProgressCompletionReplyMarkup = afterProgressCompletionReplyMarkup;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}

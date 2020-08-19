package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Update {
    private static final String MESSAGE_FIELD = "message";
    private static final String CALLBACKQUERY_FIELD = "callback_query";

    @JsonProperty(MESSAGE_FIELD)
    private Message message;
    @JsonProperty(CALLBACKQUERY_FIELD)
    private CallbackQuery callbackQuery;

    public Message getMessage() {
        return message;
    }

    public CallbackQuery getCallbackQuery() {
        return callbackQuery;
    }

    public boolean hasMessage() {
        return message != null;
    }

    public boolean hasCallbackQuery() {
        return callbackQuery != null;
    }

    @Override
    public String toString() {
        return "Update{" +
                ", message=" + message +
                ", callbackQuery=" + callbackQuery +
                '}';
    }
}

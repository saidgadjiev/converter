package ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.buttons;

import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

public class InlineKeyboardButton {

    private static final String TEXT_FIELD = "text";
    private static final String URL_FIELD = "url";
    private static final String CALLBACK_DATA_FIELD = "callback_data";

    @JsonProperty(TEXT_FIELD)
    private String text;
    @JsonProperty(URL_FIELD)
    private String url;
    @JsonProperty(CALLBACK_DATA_FIELD)
    private String callbackData;

    public InlineKeyboardButton() {
        super();
    }

    public InlineKeyboardButton(String text) {
        this.text = checkNotNull(text);
    }

    public String getText() {
        return text;
    }

    public InlineKeyboardButton setText(String text) {
        this.text = text;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public InlineKeyboardButton setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public InlineKeyboardButton setCallbackData(String callbackData) {
        this.callbackData = callbackData;
        return this;
    }

    @Override
    public String toString() {
        return "InlineKeyboardButton{" +
                "text='" + text + '\'' +
                ", url='" + url + '\'' +
                ", callbackData='" + callbackData + '\'' +
                '}';
    }
}

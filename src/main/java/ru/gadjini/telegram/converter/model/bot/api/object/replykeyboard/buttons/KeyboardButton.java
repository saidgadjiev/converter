package ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.buttons;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KeyboardButton {

    private static final String TEXT_FIELD = "text";

    @JsonProperty(TEXT_FIELD)
    private String text;

    public KeyboardButton() {
        super();
    }

    public KeyboardButton(String text) {
        super();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public KeyboardButton setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public String toString() {
        return "KeyboardButton{" +
                "text=" + text +
                '}';
    }
}

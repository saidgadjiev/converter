package ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * This object represents an inline keyboard that appears right next to the message it
 * belongs to.
 */
@JsonDeserialize
public class InlineKeyboardMarkup implements ReplyKeyboard {

    private static final String KEYBOARD_FIELD = "inline_keyboard";

    @JsonProperty(KEYBOARD_FIELD)
    private List<List<InlineKeyboardButton>> keyboard; ///< Array of button rows, each represented by an Array of Strings

    public InlineKeyboardMarkup() {
        super();
        keyboard = new ArrayList<>();
    }

    public InlineKeyboardMarkup(List<List<InlineKeyboardButton>> keyboard) {
        super();
        this.keyboard = keyboard;
    }

    public List<List<InlineKeyboardButton>> getKeyboard() {
        return keyboard;
    }

    public InlineKeyboardMarkup setKeyboard(List<List<InlineKeyboardButton>> keyboard) {
        this.keyboard = keyboard;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof InlineKeyboardMarkup)) {
            return false;
        }
        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) o;
        return Objects.equals(keyboard, inlineKeyboardMarkup.keyboard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyboard);
    }

    @Override
    public String toString() {
        return "InlineKeyboardMarkup{" +
                "inline_keyboard=" + keyboard +
                '}';
    }
}

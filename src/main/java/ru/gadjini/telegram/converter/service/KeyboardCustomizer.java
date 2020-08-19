package ru.gadjini.telegram.converter.service;

import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.converter.service.command.CommandParser;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class KeyboardCustomizer {

    private InlineKeyboardMarkup keyboardMarkup;

    public KeyboardCustomizer(InlineKeyboardMarkup replyMarkup) {
        this.keyboardMarkup = replyMarkup;
    }

    public KeyboardCustomizer removeExclude(String... exclude) {
        List<String> excludeCommands = Arrays.asList(exclude);

        for (List<InlineKeyboardButton> keyboardButtons : keyboardMarkup.getKeyboard()) {
            keyboardButtons.removeIf(inlineKeyboardButton -> {
                return excludeCommands.stream().noneMatch(excludeCmd -> inlineKeyboardButton.getCallbackData().startsWith(excludeCmd));
            });
        }

        return this;
    }

    public KeyboardCustomizer replaceButton(String oldButtonName, String newButtonName, String newButtonDesc) {
        for (List<InlineKeyboardButton> keyboardButtons : keyboardMarkup.getKeyboard()) {
            InlineKeyboardButton inlineKeyboardButton = keyboardButtons.stream().filter(new Predicate<InlineKeyboardButton>() {
                @Override
                public boolean test(InlineKeyboardButton inlineKeyboardButton) {
                    return inlineKeyboardButton.getCallbackData().startsWith(oldButtonName);
                }
            }).findFirst().orElse(null);

            if (inlineKeyboardButton != null) {
                String callbackData = inlineKeyboardButton.getCallbackData();
                int commandNameIndexOf = callbackData.indexOf(CommandParser.COMMAND_NAME_SEPARATOR);
                callbackData = callbackData.substring(commandNameIndexOf);

                inlineKeyboardButton.setText(newButtonDesc);
                inlineKeyboardButton.setCallbackData(newButtonName + callbackData);
            }
        }

        return this;
    }

    public KeyboardCustomizer remove(String... buttonNames) {
        List<String> buttons = Arrays.asList(buttonNames);

        for (List<InlineKeyboardButton> keyboardButtons : keyboardMarkup.getKeyboard()) {
            keyboardButtons.removeIf(inlineKeyboardButton -> {
                return buttons.stream().anyMatch(cmd -> inlineKeyboardButton.getCallbackData().startsWith(cmd));
            });
        }

        return this;
    }

    public boolean hasButton(String buttonName) {
        for (List<InlineKeyboardButton> keyboardButtons : keyboardMarkup.getKeyboard()) {
            if (keyboardButtons.stream().anyMatch(inlineKeyboardButton -> inlineKeyboardButton.getCallbackData().startsWith(buttonName))) {
                return true;
            }
        }

        return false;
    }

    public InlineKeyboardMarkup getKeyboardMarkup() {
        return keyboardMarkup;
    }
}

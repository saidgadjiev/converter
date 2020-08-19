package ru.gadjini.telegram.converter.model.bot.api.method.send;

public class HtmlMessage extends SendMessage {

    public HtmlMessage() {
        enableHtml(true);
    }

    public HtmlMessage(String chatId, String text) {
        super(chatId, text);
        enableHtml(true);
    }

    public HtmlMessage(Long chatId, String text) {
        super(chatId, text);
        enableHtml(true);
    }
}

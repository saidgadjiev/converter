package ru.gadjini.telegram.converter.bot.command.api;

import ru.gadjini.telegram.converter.model.bot.api.object.Message;

public interface BotCommand {
    String COMMAND_INIT_CHARACTER = "/";

    void processMessage(Message message, String[] params);

    String getCommandIdentifier();
}

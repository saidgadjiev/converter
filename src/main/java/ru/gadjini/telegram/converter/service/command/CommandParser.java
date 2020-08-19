package ru.gadjini.telegram.converter.service.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.request.RequestParams;
import ru.gadjini.telegram.converter.request.RequestParamsParser;

import java.util.Arrays;

@Service
public class CommandParser {

    public static final String COMMAND_ARG_SEPARATOR = "=";

    public static final String COMMAND_NAME_SEPARATOR = ":";

    private RequestParamsParser requestParamsParser;

    @Autowired
    public CommandParser(RequestParamsParser requestParamsParser) {
        this.requestParamsParser = requestParamsParser;
    }

    public CommandParseResult parseCallbackCommand(CallbackQuery callbackQuery) {
        String text = callbackQuery.getData();
        String[] commandSplit = text.split(COMMAND_NAME_SEPARATOR);
        RequestParams requestParams = new RequestParams();

        if (commandSplit.length > 1) {
            requestParams = requestParamsParser.parse(commandSplit[1]);
        }

        return new CommandParseResult(commandSplit[0], requestParams);
    }

    public CommandParseResult parseBotCommand(Message message) {
        String text = message.getText().trim();
        String[] commandSplit = text.split(COMMAND_ARG_SEPARATOR);
        String[] parameters = Arrays.copyOfRange(commandSplit, 1, commandSplit.length);

        return new CommandParseResult(commandSplit[0].substring(1), parameters);
    }

    public String parseBotCommandName(Message message) {
        String text = message.getText().trim();
        String[] commandSplit = text.split(COMMAND_ARG_SEPARATOR);

        return commandSplit[0].substring(1);
    }

    public static class CommandParseResult {

        private String commandName;

        private String[] parameters;

        private RequestParams requestParams;

        public CommandParseResult(String commandName, String[] parameters) {
            this.commandName = commandName;
            this.parameters = parameters;
        }

        public CommandParseResult(String commandName, RequestParams requestParams) {
            this.commandName = commandName;
            this.requestParams = requestParams;
        }

        public String getCommandName() {
            return commandName;
        }

        public String[] getParameters() {
            return parameters;
        }

        public RequestParams getRequestParams() {
            return requestParams;
        }
    }
}

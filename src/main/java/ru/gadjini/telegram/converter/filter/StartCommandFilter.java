package ru.gadjini.telegram.converter.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.CreateOrUpdateResult;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.TgMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.converter.service.CommandMessageBuilder;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.command.CommandParser;
import ru.gadjini.telegram.converter.service.command.navigator.CommandNavigator;
import ru.gadjini.telegram.converter.service.keyboard.ReplyKeyboardService;
import ru.gadjini.telegram.converter.service.message.MessageService;

@Component
public class StartCommandFilter extends BaseBotFilter {

    private CommandParser commandParser;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private CommandNavigator commandNavigator;

    private CommandMessageBuilder commandMessageBuilder;

    @Autowired
    public StartCommandFilter(CommandParser commandParser, UserService userService,
                              @Qualifier("messagelimits") MessageService messageService, LocalisationService localisationService,
                              @Qualifier("curr") ReplyKeyboardService replyKeyboardService, CommandNavigator commandNavigator, CommandMessageBuilder commandMessageBuilder) {
        this.commandParser = commandParser;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandNavigator = commandNavigator;
        this.commandMessageBuilder = commandMessageBuilder;
    }

    @Override
    public void doFilter(Update update) {
        if (isStartCommand(update)) {
            CreateOrUpdateResult createOrUpdateResult = doStart(TgMessage.from(update));

            if (createOrUpdateResult.isCreated()) {
                return;
            }
        }

        super.doFilter(update);
    }

    private boolean isStartCommand(Update update) {
        if (update.hasMessage() && update.getMessage().isCommand()) {
            String commandName = commandParser.parseBotCommandName(update.getMessage());

            return commandName.equals(CommandNames.START_COMMAND);
        }

        return false;
    }

    private CreateOrUpdateResult doStart(TgMessage message) {
        CreateOrUpdateResult createOrUpdateResult = userService.createOrUpdate(message.getUser());

        if (createOrUpdateResult.isCreated()) {
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_WELCOME,
                    new Object[]{commandMessageBuilder.getCommandsInfo(createOrUpdateResult.getUser().getLocale())},
                    createOrUpdateResult.getUser().getLocale());
            ReplyKeyboard mainMenu = replyKeyboardService.removeKeyboard(message.getChatId());
            messageService.sendMessage(
                    new HtmlMessage(message.getChatId(), text)
                            .setReplyMarkup(mainMenu)
            );

            commandNavigator.setCurrentCommand(message.getChatId(), CommandNames.START_COMMAND);
        }

        return createOrUpdateResult;
    }
}

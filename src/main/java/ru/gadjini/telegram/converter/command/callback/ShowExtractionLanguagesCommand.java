package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.impl.extraction.ExtractionByLanguageState;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Component
public class ShowExtractionLanguagesCommand implements CallbackBotCommand {

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public ShowExtractionLanguagesCommand(CommandStateService commandStateService, InlineKeyboardService inlineKeyboardService,
                                          @TgMessageLimitsControl MessageService messageService, UserService userService) {
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageService = messageService;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return ConverterCommandNames.SHOW_EXTRACTION_LANGUAGES;
    }

    @Override
    public void processCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        ExtractionByLanguageState extractionByLanguageState = commandStateService.getState(callbackQuery.getFrom().getId(),
                ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE, true, ExtractionByLanguageState.class);
        Locale localeOrDefault = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());
        messageService.editKeyboard(
                callbackQuery.getMessage().getReplyMarkup(),
                EditMessageReplyMarkup.builder()
                        .chatId(String.valueOf(callbackQuery.getFrom().getId()))
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .replyMarkup(inlineKeyboardService.getLanguagesKeyboard(extractionByLanguageState.getLanguages(), localeOrDefault))
                        .build()
        );
    }

    @Override
    public void processNonCommandCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(ConverterArg.GO_BACK.getKey())) {
            messageService.editKeyboard(
                    callbackQuery.getMessage().getReplyMarkup(),
                    EditMessageReplyMarkup.builder()
                            .chatId(String.valueOf(callbackQuery.getFrom().getId()))
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .replyMarkup(inlineKeyboardService.getLanguagesRootKeyboard(userService.getLocaleOrDefault(callbackQuery.getFrom().getId())))
                            .build()
            );
        }
    }
}

package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.ConversionReportService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Component
public class ConversionReportCommand implements CallbackBotCommand {

    private static final int ANSWER_CACHE_TIME = 30 * 24 * 60;

    private ConversionReportService fileReportService;

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public ConversionReportCommand(ConversionReportService fileReportService, @TgMessageLimitsControl MessageService messageService,
                                   UserService userService, LocalisationService localisationService) {
        this.fileReportService = fileReportService;
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
    }

    @Override
    public String getName() {
        return ConverterCommandNames.REPORT_COMMAND_NAME;
    }

    @Override
    public void processCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        int itemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());

        messageService.sendAnswerCallbackQuery(
                AnswerCallbackQuery.builder().callbackQueryId(callbackQuery.getId())
                        .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CONVERSION_REPORT, locale))
                        .cacheTime(ANSWER_CACHE_TIME)
                        .showAlert(true).build());

        fileReportService.createReport(callbackQuery.getFrom().getId(), itemId);
    }
}

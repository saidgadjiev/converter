package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.ConvertionService;
import ru.gadjini.telegram.converter.service.conversion.impl.extraction.ExtractionByLanguageState;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Component
public class ExtractionByLanguageCommand implements CallbackBotCommand {

    private ConvertionService convertionService;

    private CommandStateService commandStateService;

    private UserService userService;

    private MessageService messageService;

    @Autowired
    public ExtractionByLanguageCommand(ConvertionService convertionService, CommandStateService commandStateService,
                                       UserService userService, @TgMessageLimitsControl MessageService messageService) {
        this.convertionService = convertionService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    public String getName() {
        return ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE;
    }

    @Override
    public void processCallbackQuery(CallbackQuery callbackQuery, RequestParams requestParams) {
        String language = requestParams.getString(ConverterArg.LANGUAGE.getKey());
        ExtractionByLanguageState extractionByLanguageState = commandStateService.getState(callbackQuery.getFrom().getId(),
                getName(), true, ExtractionByLanguageState.class);
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());
        ConvertState state = createState(language, extractionByLanguageState, locale);
        convertionService.createConversion(callbackQuery.getFrom(), state, extractionByLanguageState.getTargetFormat(), locale);
        messageService.deleteMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
        commandStateService.deleteState(callbackQuery.getFrom().getId(), getName());
    }

    private ConvertState createState(String languageToExtract, ExtractionByLanguageState audioExtractionState, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setUserLanguage(locale.getLanguage());

        convertState.setMessageId(audioExtractionState.getReplyToMessageId());
        convertState.addMedia(createMedia(audioExtractionState.getFile()));

        SettingsState settingsState = new SettingsState();
        settingsState.setLanguageToExtract(languageToExtract);
        convertState.setSettings(settingsState);

        return convertState;
    }

    private MessageMedia createMedia(TgFile tgFile) {
        MessageMedia messageMedia = new MessageMedia();

        messageMedia.setFileId(tgFile.getFileId());
        messageMedia.setFormat(tgFile.getFormat());
        messageMedia.setFileName(tgFile.getFileName());
        messageMedia.setMimeType(tgFile.getMimeType());
        messageMedia.setFileSize(tgFile.getSize());
        messageMedia.setThumb(tgFile.getThumb());
        messageMedia.setSource(tgFile.getSource());
        messageMedia.setAudioTitle(tgFile.getAudioTitle());
        messageMedia.setAudioPerformer(tgFile.getAudioPerformer());
        messageMedia.setThumbFileSize(tgFile.getThumbSize());

        return messageMedia;
    }
}

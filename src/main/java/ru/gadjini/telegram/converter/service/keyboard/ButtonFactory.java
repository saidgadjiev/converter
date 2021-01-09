package ru.gadjini.telegram.converter.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.AudioCompressionMode;
import ru.gadjini.telegram.smart.bot.commons.command.impl.CallbackDelegate;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandParser;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Service
public class ButtonFactory {

    private LocalisationService localisationService;

    @Autowired
    public ButtonFactory(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public InlineKeyboardButton report(int queryItemId, Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.REPORT_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(ConverterCommandNames.REPORT_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.QUEUE_ITEM_ID.getKey(), queryItemId)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton autoAudioCompressionMode(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.AUDIO_COMPRESSION_MODE_AUTO_COMMAND_NAME, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.AUDIO_COMPRESS_MODE.getKey(), AudioCompressionMode.AUTO.name())
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton manuallyAudioCompressionMode(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.AUDIO_COMPRESSION_MODE_MANUALLY_COMMAND_NAME, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.AUDIO_COMPRESS_MODE.getKey(), AudioCompressionMode.MANUALLY.name())
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton goBackDelegate(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.GO_BACK.getKey(), "g")
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }
}

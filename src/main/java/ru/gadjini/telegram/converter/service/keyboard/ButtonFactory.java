package ru.gadjini.telegram.converter.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.smart.bot.commons.command.impl.CallbackDelegate;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandParser;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;
import java.util.Objects;

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

    public InlineKeyboardButton audioCompress(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.AUDIO_COMPRESSION_COMPRESS_COMMAND_NAME, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.COMPRESS.getKey(), "a")
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton bitrateButton(String currentBitrate, String bitrate, Locale locale) {
        String btnName = Objects.equals(currentBitrate, bitrate)
                ? localisationService.getMessage(ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties.RED_CIRCLE_ICON, locale) + bitrate
                : bitrate;

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.BITRATE.getKey(), bitrate)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton resolutionButton(String resolution) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(resolution);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.RESOLUTION.getKey(), resolution)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton editVideoButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.EDIT_VIDEO_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.EDIT_VIDEO.getKey(), false)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }
}

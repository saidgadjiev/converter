package ru.gadjini.telegram.converter.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioCodecState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.smart.bot.commons.command.impl.CallbackDelegate;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandParser;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
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

    public InlineKeyboardButton chooseResolutionButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(
                localisationService.getMessage(ConverterMessagesProperties.VEDIT_RESOLUTION_COMMAND_NAME, locale));

        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.VEDIT_CHOOSE_RESOLUTION.getKey(), true)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton chooseCrfButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(
                localisationService.getMessage(ConverterMessagesProperties.VEDIT_CRF_COMMAND_NAME, locale));

        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.VEDIT_CHOOSE_CRF.getKey(), true)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton chooseAudioCodecButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(
                localisationService.getMessage(ConverterMessagesProperties.VEDIT_AUDIO_CODEC_COMMAND_NAME, locale));

        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.VEDIT_CHOOSE_AUDIO_CODEC.getKey(), true)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton report(int queryItemId, Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(ConverterMessagesProperties.REPORT_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(ConverterCommandNames.REPORT_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.QUEUE_ITEM_ID.getKey(), queryItemId)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton audioCompress(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(ConverterMessagesProperties.AUDIO_COMPRESSION_COMPRESS_COMMAND_NAME, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.COMPRESS.getKey(), "a")
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton bitrateButton(String currentBitrate, String bitrate, Locale locale) {
        String btnName = Objects.equals(currentBitrate, bitrate)
                ? localisationService.getMessage(MessagesProperties.RED_CIRCLE_ICON, locale) + bitrate
                : bitrate;

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.BITRATE.getKey(), bitrate)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton frequencyFormat(String frequency, String currentFrequency, Locale locale) {
        String commandName = frequency + "Hz";
        String btnName = Objects.equals(currentFrequency, frequency)
                ? localisationService.getMessage(MessagesProperties.RED_CIRCLE_ICON, locale) + commandName
                : commandName;

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.COMPRESSION_FREQUENCY.getKey(), frequency)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton compressionFormatButton(Format currentFormat, Format target, Locale locale) {
        String commandName = localisationService.getMessage(target.getName().toLowerCase() + ".compression.command.description", locale);
        String btnName = Objects.equals(currentFormat, target)
                ? localisationService.getMessage(MessagesProperties.RED_CIRCLE_ICON, locale) + commandName
                : commandName;

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.COMPRESS_AUDIO)
                        .add(ConverterArg.COMPRESSION_FORMAT.getKey(), target.name())
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton extractAllButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(
                localisationService.getMessage(ConverterMessagesProperties.EXTRACT_ALL_MEDIA_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton vavMergeAudioModeButton(String nameCode, String mode, String choseMode, String modeArgName, Locale locale) {
        String btnName = Objects.equals(choseMode, mode)
                ? localisationService.getMessage(MessagesProperties.RED_CIRCLE_ICON, locale) + localisationService.getMessage(nameCode, locale)
                : localisationService.getMessage(nameCode, locale);

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.VAVMERGE)
                        .add(modeArgName, mode)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }


    public InlineKeyboardButton vavMergeButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(
                localisationService.getMessage(ConverterMessagesProperties.VAVMERGE_START_COMMAND_NAME, locale)
        );
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.VAVMERGE)
                        .add(ConverterArg.VAV_MERGE.getKey(), true)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton extractByLanguagesButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(
                localisationService.getMessage(ConverterMessagesProperties.EXTRACT_BY_LANGUAGES_MEDIA_COMMAND_DESCRIPTION, locale)
        );
        inlineKeyboardButton.setCallbackData(ConverterCommandNames.SHOW_EXTRACTION_LANGUAGES + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton extractByLanguageButton(String language) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(language);
        inlineKeyboardButton.setCallbackData(ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(ConverterArg.LANGUAGE.getKey(), language)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton goBackButton(String callbackDelegate, Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(
                localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)
        );
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, callbackDelegate)
                        .add(ConverterArg.GO_BACK.getKey(), true)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton resolutionButton(String currentResolution, String resolution, Locale locale) {
        String resolutionName = resolution;
        if (EditVideoResolutionState.DONT_CHANGE.equals(resolution)) {
            resolutionName = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_CHANGE, locale);
        }
        String btnName = Objects.equals(currentResolution, resolution)
                ? localisationService.getMessage(MessagesProperties.RED_CIRCLE_ICON, locale) + resolutionName
                : resolutionName;

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.RESOLUTION.getKey(), resolution)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton crfButton(String currentCrf, String crf, Locale locale) {
        String crfName = crf;
        if (EditVideoResolutionState.DONT_CHANGE.equals(crf)) {
            crfName = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_DONT_CHANGE, locale);
        }
        String btnName = Objects.equals(currentCrf, crf)
                ? localisationService.getMessage(MessagesProperties.RED_CIRCLE_ICON, locale) + crfName
                : crfName;

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.CRF.getKey(), crf)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton audioCodecButton(String currentCodec, String codec, Locale locale) {
        String codecName = codec;
        if (EditVideoAudioCodecState.AUTO.equals(codec)) {
            codecName = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUTO, locale);
        }
        String btnName = Objects.equals(currentCodec, codec)
                ? localisationService.getMessage(MessagesProperties.RED_CIRCLE_ICON, locale) + codecName
                : codecName;

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(btnName);
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.AUDIO_CODEC.getKey(), codec)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton editVideoButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(ConverterMessagesProperties.EDIT_VIDEO_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(CallbackDelegate.ARG_NAME, ConverterCommandNames.EDIT_VIDEO)
                        .add(ConverterArg.EDIT_VIDEO.getKey(), false)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }
}

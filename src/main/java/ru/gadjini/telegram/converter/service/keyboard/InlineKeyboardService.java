package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoCrfState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.command.bot.vavmerge.VavMergeState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.request.ConverterArg;
import ru.gadjini.telegram.converter.service.conversion.impl.VavMergeConverter;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    private SmartInlineKeyboardService smartInlineKeyboardService;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory, SmartInlineKeyboardService smartInlineKeyboardService) {
        this.buttonFactory = buttonFactory;
        this.smartInlineKeyboardService = smartInlineKeyboardService;
    }

    public InlineKeyboardMarkup getVavMergeSettingsKeyboard(VavMergeState vavMergeState, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        if (vavMergeState.getAudios().size() > 0) {
            inlineKeyboardMarkup.getKeyboard()
                    .add(List.of(buttonFactory.vavMergeAudioModeButton(ConverterMessagesProperties.MESSAGE_ADD_AUDIO_MODE,
                            VavMergeConverter.ADD_AUDIO_MODE, vavMergeState.getAudioMode(), ConverterArg.VAV_MERGE_AUDIO_MODE.getKey(), locale),
                            buttonFactory.vavMergeAudioModeButton(ConverterMessagesProperties.MESSAGE_REPLACE_AUDIO_MODE,
                                    VavMergeConverter.REPLACE_AUDIO_MODE, vavMergeState.getAudioMode(), ConverterArg.VAV_MERGE_AUDIO_MODE.getKey(), locale)));
        }
        if (vavMergeState.getSubtitles().size() > 0) {
            inlineKeyboardMarkup.getKeyboard()
                    .add(List.of(buttonFactory.vavMergeAudioModeButton(ConverterMessagesProperties.MESSAGE_ADD_SUBTITLES_MODE,
                            VavMergeConverter.ADD_SUBTITLES_MODE, vavMergeState.getSubtitlesMode(), ConverterArg.VAV_MERGE_SUBTITLES_MODE.getKey(), locale),
                            buttonFactory.vavMergeAudioModeButton(ConverterMessagesProperties.MESSAGE_REPLACE_SUBTITLES_MODE,
                                    VavMergeConverter.REPLACE_SUBTITLES_MODE, vavMergeState.getSubtitlesMode(), ConverterArg.VAV_MERGE_SUBTITLES_MODE.getKey(), locale)));
        }
        
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.vavMergeButton(locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getLanguagesRootKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard()
                .add(List.of(buttonFactory.extractByLanguagesButton(locale)));
        inlineKeyboardMarkup.getKeyboard()
                .add(List.of(buttonFactory.extractAllButton(locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getLanguagesKeyboard(List<String> languages, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        List<List<String>> lists = Lists.partition(languages, 3);
        for (List<String> list : lists) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String language : list) {
                buttons.add(buttonFactory.extractByLanguageButton(language));
            }
            inlineKeyboardMarkup.getKeyboard().add(buttons);
        }
        inlineKeyboardMarkup.getKeyboard().add(
                List.of(buttonFactory.goBackButton(ConverterCommandNames.SHOW_EXTRACTION_LANGUAGES, locale))
        );

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getVideoEditSettingsKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.chooseResolutionButton(locale),
                buttonFactory.chooseCrfButton(locale)
        ));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.editVideoButton(locale)
        ));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getVideoEditResolutionsKeyboard(String currentResolution, List<String> resolutions, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        resolutions = new ArrayList<>(resolutions);
        resolutions.remove(EditVideoCrfState.DONT_CHANGE);
        inlineKeyboardMarkup.getKeyboard()
                .add(List.of(buttonFactory.resolutionButton(currentResolution, EditVideoResolutionState.DONT_CHANGE, locale)));
        List<List<String>> lists = Lists.partition(resolutions, 3);
        for (List<String> list : lists) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String resolution : list) {
                buttons.add(buttonFactory.resolutionButton(currentResolution, resolution, locale));
            }
            inlineKeyboardMarkup.getKeyboard().add(buttons);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.goBackButton(ConverterCommandNames.EDIT_VIDEO, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getVideoEditCrfKeyboard(String currentCrf, List<String> crfs, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();
        crfs = new ArrayList<>(crfs);
        crfs.remove(EditVideoCrfState.DONT_CHANGE);
        inlineKeyboardMarkup.getKeyboard()
                .add(List.of(buttonFactory.crfButton(currentCrf, EditVideoCrfState.DONT_CHANGE, locale)));
        List<List<String>> lists = Lists.partition(crfs, 3);
        for (List<String> list : lists) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String crf : list) {
                buttons.add(buttonFactory.crfButton(currentCrf, crf, locale));
            }
            inlineKeyboardMarkup.getKeyboard().add(buttons);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.goBackButton(ConverterCommandNames.EDIT_VIDEO, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getAudioCompressionSettingsKeyboard(String currentBitrate, String currentFrequency,
                                                                    Format currentFormat,
                                                                    List<Format> compressionFormats,
                                                                    List<String> frequencies,
                                                                    List<String> bitrates, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();
        List<InlineKeyboardButton> frequencyButtons = new ArrayList<>();
        for (String frequency : frequencies.stream().sorted().collect(Collectors.toList())) {
            frequencyButtons.add(buttonFactory.frequencyFormat(frequency, currentFrequency, locale));
        }
        List<InlineKeyboardButton> compressionFormatButtons = new ArrayList<>();
        for (Format compressionFormat : compressionFormats) {
            compressionFormatButtons.add(buttonFactory.compressionFormatButton(currentFormat, compressionFormat, locale));
        }
        inlineKeyboardMarkup.getKeyboard().add(compressionFormatButtons);
        inlineKeyboardMarkup.getKeyboard().add(frequencyButtons);
        List<List<String>> lists = Lists.partition(bitrates, 3);
        for (List<String> list : lists) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String bitrate : list) {
                buttons.add(buttonFactory.bitrateButton(currentBitrate, bitrate, locale));
            }
            inlineKeyboardMarkup.getKeyboard().add(buttons);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.audioCompress(locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup reportKeyboard(int queueItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.report(queueItemId, locale)));
        return inlineKeyboardMarkup;
    }
}

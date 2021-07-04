package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
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

    public InlineKeyboardMarkup getVideoEditSettingsKeyboard(String currentResolution, List<String> resolutions, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();
        List<List<String>> lists = Lists.partition(resolutions, 3);
        for (List<String> list : lists) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String resolution : list) {
                buttons.add(buttonFactory.resolutionButton(currentResolution, resolution, locale));
            }
            inlineKeyboardMarkup.getKeyboard().add(buttons);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.editVideoButton(locale)));

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

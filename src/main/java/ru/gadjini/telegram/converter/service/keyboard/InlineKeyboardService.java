package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    private SmartInlineKeyboardService smartInlineKeyboardService;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory, SmartInlineKeyboardService smartInlineKeyboardService) {
        this.buttonFactory = buttonFactory;
        this.smartInlineKeyboardService = smartInlineKeyboardService;
    }

    public InlineKeyboardMarkup getVideoEditSettingsKeyboard(List<String> resolutions, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();
        List<List<String>> lists = Lists.partition(resolutions, 3);
        for (List<String> list : lists) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String resolution : list) {
                buttons.add(buttonFactory.resolutionButton(resolution));
            }
            inlineKeyboardMarkup.getKeyboard().add(buttons);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.editVideoButton(locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getAudioCompressionSettingsKeyboard(String currentBitrate, Format currentFormat,
                                                                    List<Format> compressionFormats, List<String> bitrates, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();
        List<InlineKeyboardButton> compressionFormatButtons = new ArrayList<>();
        for (Format compressionFormat : compressionFormats) {
            compressionFormatButtons.add(buttonFactory.compressionFormatButton(currentFormat, compressionFormat, locale));
        }
        inlineKeyboardMarkup.getKeyboard().add(compressionFormatButtons);
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

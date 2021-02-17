package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;

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
            for (String resolution : list) {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.resolutionButton(resolution)));
            }
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.editVideoButton(locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getAudioCompressionSettingsKeyboard(Format fileFormat, Format targetFormat, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();
        if (fileFormat != Format.OPUS) {
            if (targetFormat != Format.OPUS) {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.opusConversion(locale)));
            } else {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelOpusConversion(locale)));
            }
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

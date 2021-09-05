package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkColor;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkPosition;
import ru.gadjini.telegram.converter.service.conversion.format.ConversionFormatService;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartReplyKeyboardService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService.keyboardRow;
import static ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService.replyKeyboardMarkup;

@Service
@Qualifier("keyboard")
public class ReplyKeyboardServiceImpl implements ConverterReplyKeyboardService {

    private ConversionFormatService formatMapService;

    private LocalisationService localisationService;

    private SmartReplyKeyboardService smartReplyKeyboardService;

    @Autowired
    public ReplyKeyboardServiceImpl(@Lazy ConversionFormatService formatMapService,
                                    LocalisationService localisationService, SmartReplyKeyboardService smartReplyKeyboardService) {
        this.formatMapService = formatMapService;
        this.localisationService = localisationService;
        this.smartReplyKeyboardService = smartReplyKeyboardService;
    }

    @Override
    public ReplyKeyboard mainMenuKeyboard(long chatId, Locale locale) {
        return removeKeyboard(chatId);
    }

    @Override
    public ReplyKeyboardMarkup smartFileFeatureKeyboard(long chatId, Locale locale) {
        return smartReplyKeyboardService.smartFileFeatureKeyboard(locale);
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        return smartReplyKeyboardService.languageKeyboard(locale);
    }

    @Override
    public ReplyKeyboardMarkup audioCompressionKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup mergeFilesKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.CONCATENATE_COMMAND_NAME, locale),
                localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup formatsKeyboard(long chatId, Format format, Locale locale) {
        List<Format> targetFormats = new ArrayList<>(formatMapService.getTargetFormats(format));

        targetFormats.sort(Comparator.comparing(Format::getName));
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<KeyboardRow> keyboard = replyKeyboardMarkup.getKeyboard();
        List<FormatCategory> categoriesOrder = List.of(FormatCategory.COMMON, FormatCategory.VIDEO,
                FormatCategory.SUBTITLES, FormatCategory.AUDIO);

        List<Format> firstFormats = removeAndGetAtFirstFormats(targetFormats);
        boolean firstRow = true;
        for (FormatCategory formatCategory : categoriesOrder) {
            List<Format> formats = targetFormats.stream().filter(f -> f.getCategory().equals(formatCategory)).collect(Collectors.toList());
            if (firstRow) {
                formats = Stream.concat(firstFormats.stream(), formats.stream()).collect(Collectors.toList());
                firstRow = false;
            }
            addFormatsToKeyboard(keyboard, formats);
        }
        List<Format> formats = targetFormats.stream().filter(f -> !categoriesOrder.contains(f.getCategory())).collect(Collectors.toList());

        addFormatsToKeyboard(keyboard, formats);
        keyboard.add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup goBackKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup cancelKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup videoEditKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(Format.PROBE.getName()));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup mediaCutKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(
                Format.PROBE.getName(),
                localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILE_COMMAND_NAME, locale))
        );
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup vaimakeKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(
                localisationService.getMessage(ConverterMessagesProperties.VAIMAKE_COMMAND_NAME, locale),
                localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale))
        );
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup vavmergeKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(
                localisationService.getMessage(ConverterMessagesProperties.VAVMERGE_COMMAND_NAME, locale),
                localisationService.getMessage(ConverterMessagesProperties.CANCEL_FILES_COMMAND_NAME, locale))
        );
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;

    }

    @Override
    public ReplyKeyboardMarkup watermarkTypeKeyboard(long chatId, Locale locale, boolean creation) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(
                localisationService.getMessage(ConverterMessagesProperties.IMAGE_WATERMARK_COMMAND_NAME, locale),
                localisationService.getMessage(ConverterMessagesProperties.TEXT_WATERMARK_COMMAND_NAME, locale)
        ));
        if (creation) {
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));
        } else {
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));
        }

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkTextFontSizeKeyboard(long chatId, Locale locale, List<String> fontSizes) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<List<String>> lists = Lists.partition(fontSizes, 6);
        for (List<String> list : lists) {
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(list.toArray(String[]::new)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkPositionKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<List<VideoWatermarkPosition>> lists = Lists.partition(Arrays.asList(VideoWatermarkPosition.values()), 3);
        for (List<VideoWatermarkPosition> list : lists) {
            List<String> buttons = list.stream()
                    .map(v -> localisationService.getMessage(v.name().toLowerCase().replace("_", "."), locale))
                    .collect(Collectors.toList());
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(buttons.toArray(String[]::new)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkTextColorKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<List<VideoWatermarkColor>> lists = Lists.partition(Arrays.asList(VideoWatermarkColor.values()), 3);
        for (List<VideoWatermarkColor> list : lists) {
            List<String> buttons = list.stream()
                    .map(v -> localisationService.getMessage(v.name().toLowerCase() + ".color", locale))
                    .collect(Collectors.toList());
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(buttons.toArray(String[]::new)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkImageSizeKeyboard(long chatId, Locale locale, List<String> widths) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<List<String>> lists = Lists.partition(widths, 4);
        for (List<String> list : lists) {
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(list.toArray(String[]::new)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkImageTransparencyKeyboard(long chatId, Locale locale, List<String> transparencies) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<List<String>> lists = Lists.partition(transparencies, 5);
        for (List<String> list : lists) {
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(list.toArray(String[]::new)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkImageKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkTextKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup watermarkOkKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.CHANGE_WATERMARK_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(ConverterMessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        return smartReplyKeyboardService.removeKeyboard();
    }

    private void addFormatsToKeyboard(List<KeyboardRow> keyboard, List<Format> formats) {
        List<List<Format>> lists = Lists.partition(formats, 3);
        for (List<Format> list : lists) {
            keyboard.add(keyboardRow(list.stream().map(Format::getName).toArray(String[]::new)));
        }
    }

    private List<Format> removeAndGetAtFirstFormats(List<Format> targetFormats) {
        List<Format> formats = new ArrayList<>();
        if (targetFormats.remove(Format.COMPRESS)) {
            formats.add(Format.COMPRESS);
        }
        if (targetFormats.remove(Format.UPLOAD)) {
            formats.add(Format.UPLOAD);
        }
        if (targetFormats.remove(Format.STREAM)) {
            formats.add(Format.STREAM);
        }
        if (targetFormats.remove(Format.MUTE)) {
            formats.add(Format.MUTE);
        }
        if (targetFormats.remove(Format.SQUARE)) {
            formats.add(Format.SQUARE);
        }
        if (targetFormats.remove(Format.VIDEO_NOTE)) {
            formats.add(Format.VIDEO_NOTE);
        }
        if (targetFormats.remove(Format.VOICE)) {
            formats.add(Format.VOICE);
        }
        if (targetFormats.remove(Format.PROBE)) {
            formats.add(Format.PROBE);
        }

        return formats;
    }
}

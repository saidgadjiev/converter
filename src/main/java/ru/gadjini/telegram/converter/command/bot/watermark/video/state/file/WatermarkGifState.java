package ru.gadjini.telegram.converter.command.bot.watermark.video.state.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.WatermarkStateName;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.Locale;
import java.util.Set;

@Component
public class WatermarkGifState extends BaseWatermarkFileState {

    private static final Set<Format> GIF_FORMATS = Set.of(Format.GIF, Format.MP4);

    private LocalisationService localisationService;

    @Autowired
    public WatermarkGifState(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    protected String getWelcomeMessageCode() {
        return ConverterMessagesProperties.MESSAGE_WATERMARK_GIF_WELCOME;
    }

    @Override
    protected void validate(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat() == null
                || !GIF_FORMATS.contains(media.getFormat())) {
            throw new UserException(localisationService.getMessage(
                    ConverterMessagesProperties.MESSAGE_WATERMARK_GIF_WELCOME, locale));
        }
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_GIF_STATE;
    }
}

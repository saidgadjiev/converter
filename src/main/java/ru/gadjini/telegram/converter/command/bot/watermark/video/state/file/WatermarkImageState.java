package ru.gadjini.telegram.converter.command.bot.watermark.video.state.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.bot.watermark.video.state.WatermarkStateName;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.Locale;

@Component
public class WatermarkImageState extends BaseWatermarkFileState {

    private LocalisationService localisationService;

    @Autowired
    public WatermarkImageState(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    protected void validate(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat() == null || media.getFormat().getCategory() != FormatCategory.IMAGES) {
            throw new UserException(localisationService.getMessage(
                    ConverterMessagesProperties.MESSAGE_WATERMARK_IMAGE_WELCOME, locale));
        }
    }

    @Override
    protected String getWelcomeMessageCode() {
        return ConverterMessagesProperties.MESSAGE_WATERMARK_IMAGE_WELCOME;
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_IMAGE;
    }
}

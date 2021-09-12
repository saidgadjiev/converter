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
public class WatermarkVideoState extends BaseWatermarkFileState {

    private LocalisationService localisationService;

    @Autowired
    public WatermarkVideoState(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    protected String getWelcomeMessageCode() {
        return ConverterMessagesProperties.MESSAGE_WATERMARK_VIDEO_WELCOME;
    }

    @Override
    protected void validate(MessageMedia media, Locale locale) {
        if (media == null || media.getFormat() == null
                || media.getFormat().getCategory() != FormatCategory.VIDEO) {
            throw new UserException(localisationService.getMessage(
                    ConverterMessagesProperties.MESSAGE_WATERMARK_VIDEO_WELCOME, locale));
        }
    }

    @Override
    public WatermarkStateName getName() {
        return WatermarkStateName.WATERMARK_VIDEO_STATE;
    }
}

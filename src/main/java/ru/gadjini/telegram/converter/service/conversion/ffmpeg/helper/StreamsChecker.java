package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;

import java.util.List;
import java.util.Locale;

@Component
public class StreamsChecker {

    private LocalisationService localisationService;

    @Autowired
    public StreamsChecker(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public void checkVideoStreamsExistence(List<FFprobeDevice.FFProbeStream> streams, Locale locale) {
        if (streams.stream().noneMatch(f -> f.getCodecType().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE))) {
            throw new UserException(localisationService.getMessage(
                    ConverterMessagesProperties.MESSAGE_NO_VIDEO_STREAMS, locale
            ));
        }
    }
}

package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;

import java.util.Set;

@Component
public class FFmpegImageStreamDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegImageStreamDetector.class);

    private static final Set<String> IMAGE_CODECS = Set.of(FFmpegCommand.BMP, FFmpegCommand.PNG, FFmpegCommand.MJPEG);

    private FormatService formatService;

    @Autowired
    public FFmpegImageStreamDetector(FormatService formatService) {
        this.formatService = formatService;
    }

    public boolean isImageStream(FFprobeDevice.FFProbeStream stream) {
        if (StringUtils.isNotBlank(stream.getCodecName()) && IMAGE_CODECS.contains(stream.getCodecName())) {
            return true;
        }
        try {
            Format format = formatService.getFormat(stream.getFileName(), stream.getMimeType());

            if (format != null && format.getCategory() == FormatCategory.IMAGES) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage());
        }

        return false;
    }
}

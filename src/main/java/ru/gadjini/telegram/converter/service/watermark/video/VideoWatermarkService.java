package ru.gadjini.telegram.converter.service.watermark.video;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;

@Service
public class VideoWatermarkService {

    public boolean isExistsWatermark(int userId) {
        return false;
    }

    public void createOrUpdate(VideoWatermarkSettings videoWatermarkSettings) {

    }
}

package ru.gadjini.telegram.converter.service.watermark.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.command.bot.watermark.video.settings.VideoWatermarkSettings;
import ru.gadjini.telegram.converter.dao.VideoWatermarkDao;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;

@Service
public class VideoWatermarkService {

    private VideoWatermarkDao videoWatermarkDao;

    @Autowired
    public VideoWatermarkService(VideoWatermarkDao videoWatermarkDao) {
        this.videoWatermarkDao = videoWatermarkDao;
    }

    public VideoWatermark getWatermark(long userId) {
        return videoWatermarkDao.getWatermark(userId);
    }

    public boolean isExistsWatermark(long userId) {
        return videoWatermarkDao.isExists(userId);
    }

    public void createOrUpdate(long userId, VideoWatermarkSettings videoWatermarkSettings) {
        VideoWatermark videoWatermark = new VideoWatermark();
        videoWatermark.setUserId(userId);
        videoWatermark.setWatermarkType(videoWatermarkSettings.getWatermarkType());
        videoWatermark.setText(videoWatermarkSettings.getText());
        if (videoWatermarkSettings.getWatermarkType().equals(VideoWatermarkType.IMAGE)) {
            videoWatermark.setImage(videoWatermarkSettings.getImage().toTgFile());
        }
        videoWatermark.setFontSize(videoWatermarkSettings.getFontSize());
        videoWatermark.setColor(videoWatermarkSettings.getColor());
        videoWatermark.setWatermarkPosition(videoWatermarkSettings.getWatermarkPosition());
        videoWatermark.setImageHeight(videoWatermarkSettings.getImageHeight());
        videoWatermark.setTransparency(videoWatermarkSettings.getTransparency());

        videoWatermarkDao.createOrUpdate(videoWatermark);
    }
}

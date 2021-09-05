package ru.gadjini.telegram.converter.service.watermark.audio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.settings.AudioWatermarkSettings;
import ru.gadjini.telegram.converter.dao.AudioWatermarkQueueDao;
import ru.gadjini.telegram.converter.domain.watermark.audio.AudioWatermark;

@Service
public class AudioWatermarkService {

    private AudioWatermarkQueueDao audioWatermarkQueueDao;

    @Autowired
    public AudioWatermarkService(AudioWatermarkQueueDao audioWatermarkQueueDao) {
        this.audioWatermarkQueueDao = audioWatermarkQueueDao;
    }

    public AudioWatermark getWatermark(long userId) {
        return audioWatermarkQueueDao.getWatermark(userId);
    }

    public boolean isExistsWatermark(long userId) {
        return audioWatermarkQueueDao.isExists(userId);
    }

    public void createOrUpdate(long userId, AudioWatermarkSettings audioWatermarkSettings) {
        AudioWatermark audioWatermark = new AudioWatermark();
        audioWatermark.setUserId(userId);
        audioWatermark.setAudio(audioWatermarkSettings.getAudio().toTgFile());

        audioWatermarkQueueDao.createOrUpdate(audioWatermark);
    }
}

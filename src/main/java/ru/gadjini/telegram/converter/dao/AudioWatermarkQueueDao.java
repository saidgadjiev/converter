package ru.gadjini.telegram.converter.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.domain.watermark.audio.AudioWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class AudioWatermarkQueueDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public AudioWatermarkQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AudioWatermark getWatermark(long userId) {
        return jdbcTemplate.query(
                "SELECT *, (audio).* FROM audio_watermark WHERE user_id = ?",
                ps -> ps.setLong(1, userId),
                rs -> rs.next() ? map(rs) : null
        );
    }

    public Boolean isExists(long userId) {
        return jdbcTemplate.query(
                "SELECT TRUE as res FROM audio_watermark WHERE user_id = ?",
                ps -> ps.setLong(1, userId),
                rs -> rs.next() ? true : false
        );
    }

    public void createOrUpdate(AudioWatermark audioWatermark) {
        jdbcTemplate.update(
                "INSERT INTO audio_watermark(user_id, audio) " +
                        "VALUES (?, ?) ON CONFLICT (user_id) DO UPDATE " +
                        "SET audio = excluded.audio",
                ps -> {
                    ps.setLong(1, audioWatermark.getUserId());
                    ps.setObject(2, audioWatermark.getAudio().sqlObject());
                }
        );
    }

    private AudioWatermark map(ResultSet rs) throws SQLException {
        AudioWatermark audioWatermark = new AudioWatermark();
        audioWatermark.setUserId(rs.getInt(VideoWatermark.USER_ID));

        TgFile file = new TgFile();
        file.setFileId(rs.getString(TgFile.FILE_ID));
        file.setSize(rs.getLong(TgFile.SIZE));
        file.setFormat(Format.valueOf(rs.getString(TgFile.FORMAT)));
        audioWatermark.setAudio(file);

        return audioWatermark;
    }
}

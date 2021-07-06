package ru.gadjini.telegram.converter.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;

import java.sql.Types;

@Repository
public class VideoWatermarkDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public VideoWatermarkDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Boolean isExists(int userId) {
        return jdbcTemplate.query(
                "SELECT TRUE as res FROM video_watermark WHERE user_id = ?",
                ps -> ps.setInt(1, userId),
                rs -> rs.next() ? true : false
        );
    }

    public void createOrUpdate(VideoWatermark videoWatermark) {
        jdbcTemplate.update(
                "INSERT INTO video_watermark(user_id, type, position, wtext, image, font_size, color) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE " +
                        "SET type = excluded.type, position = excluded.position, wtext = excluded.wtext, " +
                        "image = excluded.image, font_size = excluded.font_size, color = excluded.color",
                ps -> {
                    ps.setInt(1, videoWatermark.getUserId());
                    ps.setString(2, videoWatermark.getWatermarkType().name());
                    ps.setString(3, videoWatermark.getWatermarkPosition().name());
                    if (videoWatermark.getWatermarkType().equals(VideoWatermarkType.TEXT)) {
                        ps.setString(4, videoWatermark.getText());
                        ps.setNull(5, Types.OTHER);
                        ps.setString(6, videoWatermark.getFontSize());
                        ps.setString(7, videoWatermark.getColor().name());
                    } else {
                        ps.setNull(4, Types.VARCHAR);
                        ps.setObject(5, videoWatermark.getImage().sqlObject());
                        ps.setNull(6, Types.VARCHAR);
                        ps.setNull(7, Types.VARCHAR);
                    }
                }
        );
    }
}

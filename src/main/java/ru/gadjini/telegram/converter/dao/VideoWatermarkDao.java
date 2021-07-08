package ru.gadjini.telegram.converter.dao;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkColor;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkPosition;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@Repository
public class VideoWatermarkDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public VideoWatermarkDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public VideoWatermark getWatermark(int userId) {
        return jdbcTemplate.query(
                "SELECT *, (image).* FROM video_watermark WHERE user_id = ?",
                ps -> ps.setInt(1, userId),
                rs -> rs.next() ? map(rs) : null
        );
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
                "INSERT INTO video_watermark(user_id, type, position, wtext, image, font_size, color, image_height, transparency) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE " +
                        "SET type = excluded.type, position = excluded.position, wtext = excluded.wtext, " +
                        "image = excluded.image, font_size = excluded.font_size, color = excluded.color, " +
                        "image_height = excluded.image_height," +
                        "transparency = excluded.transparency",
                ps -> {
                    ps.setInt(1, videoWatermark.getUserId());
                    ps.setString(2, videoWatermark.getWatermarkType().name());
                    ps.setString(3, videoWatermark.getWatermarkPosition().name());
                    if (videoWatermark.getWatermarkType().equals(VideoWatermarkType.TEXT)) {
                        ps.setString(4, videoWatermark.getText());
                        ps.setNull(5, Types.OTHER);
                        ps.setInt(6, videoWatermark.getFontSize());
                        ps.setString(7, videoWatermark.getColor().name());
                        ps.setNull(8, Types.INTEGER);
                        ps.setNull(9, Types.VARCHAR);
                    } else {
                        ps.setNull(4, Types.VARCHAR);
                        ps.setObject(5, videoWatermark.getImage().sqlObject());
                        ps.setNull(6, Types.INTEGER);
                        ps.setNull(7, Types.VARCHAR);
                        ps.setInt(8, videoWatermark.getImageHeight());
                        ps.setString(9, videoWatermark.getTransparency());
                    }
                }
        );
    }

    private VideoWatermark map(ResultSet rs) throws SQLException {
        VideoWatermark videoWatermark = new VideoWatermark();
        videoWatermark.setUserId(rs.getInt(VideoWatermark.USER_ID));

        String p = rs.getString(VideoWatermark.POSITION);
        if (StringUtils.isNotBlank(p)) {
            videoWatermark.setWatermarkPosition(VideoWatermarkPosition.valueOf(p));
        }
        String c = rs.getString(VideoWatermark.COLOR);
        if (StringUtils.isNotBlank(c)) {
            videoWatermark.setColor(VideoWatermarkColor.valueOf(c));
        }
        int fontSize = rs.getInt(VideoWatermark.FONT_SIZE);
        if (rs.wasNull()) {
            videoWatermark.setFontSize(fontSize);
        }
        videoWatermark.setText(rs.getString(VideoWatermark.TEXT));
        videoWatermark.setWatermarkType(VideoWatermarkType.valueOf(rs.getString(VideoWatermark.TYPE)));

        int imageWidth = rs.getInt(VideoWatermark.IMAGE_HEIGHT);
        if (rs.wasNull()) {
            videoWatermark.setImageHeight(null);
        } else {
            videoWatermark.setImageHeight(imageWidth);
        }

        String imageFileId = rs.getString(TgFile.FILE_ID);
        if (StringUtils.isNotBlank(imageFileId)) {
            TgFile file = new TgFile();
            file.setFileId(imageFileId);
            file.setSize(rs.getLong(TgFile.SIZE));
            file.setFormat(Format.valueOf(rs.getString(TgFile.FORMAT)));
            videoWatermark.setImage(file);
        }
        videoWatermark.setTransparency(rs.getString(VideoWatermark.TRANSPARENCY));

        return videoWatermark;
    }
}

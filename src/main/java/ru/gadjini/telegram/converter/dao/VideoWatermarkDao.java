package ru.gadjini.telegram.converter.dao;

import com.aspose.imaging.internal.jj.V;
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
                "INSERT INTO video_watermark(user_id, type, position, wtext, image, font_size, color, image_height) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE " +
                        "SET type = excluded.type, position = excluded.position, wtext = excluded.wtext, " +
                        "image = excluded.image, font_size = excluded.font_size, color = excluded.color, " +
                        "image_height = excluded.image_height",
                ps -> {
                    ps.setInt(1, videoWatermark.getUserId());
                    ps.setString(2, videoWatermark.getWatermarkType().name());
                    ps.setString(3, videoWatermark.getWatermarkPosition().name());
                    if (videoWatermark.getWatermarkType().equals(VideoWatermarkType.TEXT)) {
                        ps.setString(4, videoWatermark.getText());
                        ps.setNull(5, Types.OTHER);
                        ps.setString(6, videoWatermark.getFontSize());
                        ps.setString(7, videoWatermark.getColor().name());
                        ps.setNull(8, Types.INTEGER);
                    } else {
                        ps.setNull(4, Types.VARCHAR);
                        ps.setObject(5, videoWatermark.getImage().sqlObject());
                        ps.setNull(6, Types.VARCHAR);
                        ps.setNull(7, Types.VARCHAR);
                        ps.setInt(8, videoWatermark.getImageWidth());
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
        videoWatermark.setFontSize(rs.getString(VideoWatermark.FONT_SIZE));
        videoWatermark.setText(rs.getString(VideoWatermark.TEXT));
        videoWatermark.setWatermarkType(VideoWatermarkType.valueOf(rs.getString(VideoWatermark.TYPE)));

        int imageWidth = rs.getInt(VideoWatermark.IMAGE_WIDTH);
        if (rs.wasNull()) {
            videoWatermark.setImageWidth(null);
        } else {
            videoWatermark.setImageWidth(imageWidth);
        }

        String imageFileId = rs.getString(TgFile.FILE_ID);
        if (StringUtils.isNotBlank(imageFileId)) {
            TgFile file = new TgFile();
            file.setFileId(imageFileId);
            file.setSize(rs.getLong(TgFile.SIZE));
            file.setFormat(Format.valueOf(rs.getString(TgFile.FORMAT)));
            videoWatermark.setImage(file);
        }

        return videoWatermark;
    }
}

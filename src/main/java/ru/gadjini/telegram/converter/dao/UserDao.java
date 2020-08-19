package ru.gadjini.telegram.converter.dao;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.domain.TgUser;

import java.sql.Statement;
import java.sql.Types;
import java.util.Locale;

@Repository
public class UserDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int updateActivity(int userId) {
        return jdbcTemplate.update(
                "UPDATE tg_user SET last_activity_at = now() WHERE user_id = ?",
                ps -> ps.setInt(1, userId)
        );
    }

    public String createOrUpdate(TgUser user) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO tg_user(user_id, username, locale, original_locale) VALUES (?, ?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET " +
                                    "last_activity_at = now(), username = excluded.username, original_locale = excluded.original_locale, blocked = false " +
                                    "RETURNING CASE WHEN XMAX::text::int > 0 THEN 'updated' ELSE 'inserted' END AS state",
                            Statement.RETURN_GENERATED_KEYS
                    );

                    ps.setInt(1, user.getUserId());
                    if (StringUtils.isBlank(user.getUsername())) {
                        ps.setNull(2, Types.NULL);
                    } else {
                        ps.setString(2, user.getUsername());
                    }
                    ps.setString(3, user.getLanguageCode());
                    if (StringUtils.isBlank(user.getOriginalLocale())) {
                        ps.setNull(4, Types.NULL);
                    } else {
                        ps.setString(4, user.getOriginalLocale());
                    }

                    return ps;
                },
                keyHolder
        );

        return (String) keyHolder.getKeys().get("state");
    }

    public void updateLocale(int userId, Locale locale) {
        jdbcTemplate.update(
                "UPDATE tg_user SET locale = ? WHERE user_id = ?",
                ps -> {
                    ps.setString(1, locale.getLanguage());
                    ps.setInt(2, userId);
                }
        );
    }

    public void blockUser(int userId) {
        jdbcTemplate.update(
                "UPDATE tg_user SET blocked = true WHERE user_id = " + userId
        );
    }

    public String getLocale(int userId) {
        return jdbcTemplate.query(
                "SELECT locale FROM tg_user WHERE user_id = ?",
                ps -> ps.setInt(1, userId),
                rs -> {
                    if (rs.next()) {
                        return rs.getString(TgUser.LOCALE);
                    }

                    return null;
                }
        );
    }
}

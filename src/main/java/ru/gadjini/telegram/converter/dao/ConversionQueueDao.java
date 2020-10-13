package ru.gadjini.telegram.converter.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.utils.JdbcUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.domain.TgUser;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.sql.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.converter.domain.ConversionQueueItem.TYPE;

@Repository
public class ConversionQueueDao {

    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;

    private Set<Format> formatsSet = new HashSet<>();

    private FileLimitProperties fileLimitProperties;

    @Autowired
    public ConversionQueueDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                              Map<FormatCategory, Map<List<Format>, List<Format>>> formats,
                              FileLimitProperties fileLimitProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.fileLimitProperties = fileLimitProperties;
        for (Format value : Format.values()) {
            if (formats.containsKey(value.getCategory())) {
                formatsSet.add(value);
            }
        }
    }

    public void create(ConversionQueueItem queueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO " + TYPE + " (user_id, files, reply_to_message_id, target_format, status, last_run_at, started_at)\n" +
                            "    VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *", Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, queueItem.getUserId());

                    Object[] files = queueItem.getFiles().stream().map(TgFile::sqlObject).toArray();
                    Array array = con.createArrayOf(TgFile.TYPE, files);
                    ps.setArray(2, array);

                    ps.setInt(3, queueItem.getReplyToMessageId());
                    ps.setString(4, queueItem.getTargetFormat().name());
                    ps.setInt(5, queueItem.getStatus().getCode());
                    if (queueItem.getLastRunAt() != null) {
                        ps.setTimestamp(6, Timestamp.valueOf(queueItem.getLastRunAt().toLocalDateTime()));
                    } else {
                        ps.setNull(6, Types.TIMESTAMP);
                    }
                    if (queueItem.getStatedAt() != null) {
                        ps.setTimestamp(7, Timestamp.valueOf(queueItem.getStatedAt().toLocalDateTime()));
                    } else {
                        ps.setNull(7, Types.TIMESTAMP);
                    }

                    return ps;
                },
                keyHolder
        );

        int id = ((Number) keyHolder.getKeys().get(ConversionQueueItem.ID)).intValue();
        queueItem.setId(id);
    }

    public Integer getPlaceInQueue(int id, SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COALESCE(place_in_queue, 1) as place_in_queue\n" +
                        "FROM (SELECT id, row_number() over (ORDER BY created_at) AS place_in_queue\n" +
                        "      FROM " + TYPE + " c, unnest(c.files) cf\n" +
                        "      WHERE status = 0\n" +
                        "        AND files[1].format IN (" + inFormats() + ")\n" +
                        "        GROUP BY c.id HAVING sum(cf.size)" + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ?\n" +
                        ") as file_q\n" +
                        "WHERE id = ?",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
                },
                rs -> {
                    if (rs.next()) {
                        return rs.getInt(ConversionQueueItem.PLACE_IN_QUEUE);
                    }

                    return 1;
                }
        );
    }

    public Long count(ConversionQueueItem.Status status) {
        return jdbcTemplate.query(
                "SELECT COUNT(*) as cnt FROM conversion_queue WHERE status = ? AND files[1].format IN(" + inFormats() + ")",
                ps -> ps.setInt(1, status.getCode()),
                rs -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    public List<ConversionQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH queue_items AS (\n" +
                        "    UPDATE " + TYPE + " SET status = 1, last_run_at = now(), " +
                        "started_at = COALESCE(started_at, now()) WHERE id IN (\n" +
                        "        SELECT id\n" +
                        "        FROM " + TYPE + " c, unnest(c.files) cf WHERE c.status = 0 AND files[1].format IN(" + inFormats() + ")" +
                        "        GROUP BY c.id, c.created_at\n" +
                        "        HAVING SUM(cf.size) " + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ?\n" +
                        "        ORDER BY c.created_at\n" +
                        "        LIMIT ?)\n" +
                        "    RETURNING *\n" +
                        ")\n" +
                        "SELECT cv.*, cc.files_json, 1 as place_in_queue\n" +
                        "FROM queue_items cv INNER JOIN (SELECT id, json_agg(files) as files_json FROM conversion_queue WHERE status = 0 GROUP BY id) cc ON cv.id = cc.id\n" +
                        "ORDER BY cv.created_at",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public void resetProcessing() {
        jdbcTemplate.update(
                "UPDATE " + TYPE + " SET status = 0 WHERE status = 1"
        );
    }

    public void updateException(int id, int status, String exception) {
        jdbcTemplate.update(
                "UPDATE " + TYPE + " SET exception = ?, status = ?, suppress_user_exceptions = true, completed_at = now() WHERE id = ?",
                ps -> {
                    ps.setString(1, exception);
                    ps.setInt(2, status);
                    ps.setInt(3, id);
                }
        );
    }

    public void updateException(int id, String exception) {
        jdbcTemplate.update(
                "UPDATE " + TYPE + " SET exception = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, exception);
                    ps.setInt(2, id);
                }
        );
    }

    public Long getTodayConversionsCount() {
        return jdbcTemplate.query(
                "SELECT count(*) as cnt FROM conversion_queue WHERE completed_at::date = current_date AND status = 3",
                rs -> rs.next() ? rs.getLong("cnt") : -1
        );
    }

    public void updateCompletedAt(int id, int status) {
        jdbcTemplate.update(
                "UPDATE " + TYPE + " SET status = ?, completed_at = now() WHERE id = ?",
                ps -> {
                    ps.setInt(1, status);
                    ps.setInt(2, id);
                }
        );
    }


    public List<ConversionQueueItem> deleteProcessingOrWaitingByUserId(int userId) {
        return jdbcTemplate.query("WITH del AS(DELETE FROM " + TYPE + " WHERE user_id = ? AND status IN (0, 1) RETURNING *) SELECT id, status, json_agg(files) as files_json FROM del GROUP BY id, status",
                ps -> ps.setInt(1, userId),
                (rs, rowNum) -> map(rs)
        );
    }

    public ConversionQueueItem delete(int id) {
        return jdbcTemplate.query(
                "WITH del AS(DELETE FROM " + TYPE + " WHERE id = ? RETURNING *) SELECT id, status, json_agg(files) as files_json FROM del GROUP BY id, status",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

                        fileQueueItem.setId(rs.getInt(ConversionQueueItem.ID));
                        fileQueueItem.setStatus(ConversionQueueItem.Status.fromCode(rs.getInt(ConversionQueueItem.STATUS)));
                        fileQueueItem.setFiles(mapFiles(rs));

                        return fileQueueItem;
                    }

                    return null;
                }
        );
    }

    public boolean exists(int id) {
        return BooleanUtils.toBoolean(jdbcTemplate.query(
                "SELECT TRUE FROM conversion_queue WHERE id =?",
                ps -> {
                    ps.setInt(1, id);
                },
                ResultSet::next
        ));
    }

    public void setResultFileId(int id, String fileId) {
        jdbcTemplate.update("UPDATE conversion_queue SET result_file_id = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, fileId);
                    ps.setInt(2, id);
                });
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE conversion_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public void setSuppressUserExceptions(int id, boolean suppressUserExceptions) {
        jdbcTemplate.update("UPDATE conversion_queue SET suppress_user_exceptions = ? WHERE id = ?",
                ps -> {
                    ps.setBoolean(1, suppressUserExceptions);
                    ps.setInt(2, id);
                });
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        jdbcTemplate.update("UPDATE conversion_queue SET progress_message_id = ? WHERE id = ?",
                ps -> {
                    ps.setInt(1, progressMessageId);
                    ps.setInt(2, id);
                });
    }

    public SmartExecutorService.JobWeight getWeight(int id) {
        Long size = jdbcTemplate.query(
                "SELECT sum(cf.size) as sm FROm conversion_queue cv, unnest(cv.files) cf WHERE id = ?",
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getLong("sm") : null
        );

        return size == null ? null : size > fileLimitProperties.getLightFileMaxWeight() ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
    }

    public ConversionQueueItem getById(int id) {
        SmartExecutorService.JobWeight weight = getWeight(id);

        if (weight == null) {
            return null;
        }
        return jdbcTemplate.query(
                "SELECT f.*, COALESCE(queue_place.place_in_queue, 1) as place_in_queue, cc.files_json\n" +
                        "FROM " + TYPE + " f\n" +
                        "         LEFT JOIN (SELECT id, row_number() over (ORDER BY created_at) as place_in_queue\n" +
                        "                     FROM " + TYPE + " c, unnest(c.files) cf\n" +
                        "                     WHERE status = 0 AND files[1].format IN (" + inFormats() + ")" +
                        "        GROUP BY c.id HAVING sum(cf.size) " + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ?\n" +
                        ") queue_place ON f.id = queue_place.id\n" +
                        "         INNER JOIN (SELECT id, json_agg(files) as files_json FROM conversion_queue WHERE id = ? GROUP BY id) cc ON f.id = cc.id\n" +
                        "WHERE f.id = ?\n",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
                    ps.setInt(3, id);
                },
                rs -> {
                    if (rs.next()) {
                        return map(rs);
                    }

                    return null;
                }
        );
    }

    private ConversionQueueItem map(ResultSet rs) throws SQLException {
        Set<String> columns = JdbcUtils.getColumnNames(rs.getMetaData());
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setId(rs.getInt(ConversionQueueItem.ID));
        fileQueueItem.setReplyToMessageId(rs.getInt(ConversionQueueItem.REPLY_TO_MESSAGE_ID));
        fileQueueItem.setUserId(rs.getInt(ConversionQueueItem.USER_ID));
        fileQueueItem.setSuppressUserExceptions(rs.getBoolean(ConversionQueueItem.SUPPRESS_USER_EXCEPTIONS));
        fileQueueItem.setResultFileId(rs.getString(ConversionQueueItem.RESULT_FILE_ID));

        TgUser user = new TgUser();
        user.setUserId(fileQueueItem.getUserId());
        fileQueueItem.setUser(user);

        fileQueueItem.setFiles(mapFiles(rs));

        fileQueueItem.setTargetFormat(Format.valueOf(rs.getString(ConversionQueueItem.TARGET_FORMAT)));
        fileQueueItem.setMessage(rs.getString(ConversionQueueItem.MESSAGE));
        fileQueueItem.setProgressMessageId(rs.getInt(ConversionQueueItem.PROGRESS_MESSAGE_ID));
        Timestamp lastRunAt = rs.getTimestamp(ConversionQueueItem.LAST_RUN_AT);
        if (lastRunAt != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(lastRunAt.toLocalDateTime(), ZoneOffset.UTC);
            fileQueueItem.setLastRunAt(zonedDateTime);
        }
        fileQueueItem.setStatus(ConversionQueueItem.Status.fromCode(rs.getInt(ConversionQueueItem.STATUS)));
        if (columns.contains(ConversionQueueItem.PLACE_IN_QUEUE)) {
            fileQueueItem.setPlaceInQueue(rs.getInt(ConversionQueueItem.PLACE_IN_QUEUE));
        }

        return fileQueueItem;
    }

    private List<TgFile> mapFiles(ResultSet rs) throws SQLException {
        PGobject jsonArr = (PGobject) rs.getObject("files_json");
        if (jsonArr != null) {
            try {
                List<List<TgFile>> lists = objectMapper.readValue(jsonArr.getValue(), new TypeReference<>() {
                });

                return lists.iterator().next();
            } catch (JsonProcessingException e) {
                throw new SQLException(e);
            }
        }

        return null;
    }

    private String inFormats() {
        return formatsSet.stream().map(f -> "'" + f.name() + "'").collect(Collectors.joining(", "));
    }
}

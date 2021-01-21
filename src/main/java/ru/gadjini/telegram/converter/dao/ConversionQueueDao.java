package ru.gadjini.telegram.converter.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.domain.ConversionReport;
import ru.gadjini.telegram.converter.utils.JdbcUtils;
import ru.gadjini.telegram.smart.bot.commons.dao.QueueDao;
import ru.gadjini.telegram.smart.bot.commons.dao.WorkQueueDaoDelegate;
import ru.gadjini.telegram.smart.bot.commons.domain.DownloadQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.sql.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.converter.domain.ConversionQueueItem.TYPE;

@Repository
public class ConversionQueueDao implements WorkQueueDaoDelegate<ConversionQueueItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionQueueDao.class);

    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;

    private Set<Format> formatsSet = new HashSet<>();

    private FileLimitProperties fileLimitProperties;

    private Gson gson;

    @Value("${converter:all}")
    private String converter;

    @Autowired
    public ConversionQueueDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                              Map<FormatCategory, Map<List<Format>, List<Format>>> formats,
                              FileLimitProperties fileLimitProperties, Gson gson) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.fileLimitProperties = fileLimitProperties;
        this.gson = gson;
        for (Format value : Format.values()) {
            if (formats.containsKey(value.getCategory())) {
                formatsSet.add(value);
            }
        }
        LOGGER.debug("Light file weight({})", MemoryUtils.humanReadableByteCount(fileLimitProperties.getLightFileMaxWeight()));
    }

    public void create(ConversionQueueItem queueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO " + TYPE + " (user_id, files, reply_to_message_id, target_format, status, extra)\n" +
                            "    VALUES (?, ?, ?, ?, ?, ?) RETURNING *", Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, queueItem.getUserId());

                    Object[] files = queueItem.getFiles().stream().map(TgFile::sqlObject).toArray();
                    Array array = con.createArrayOf(TgFile.TYPE, files);
                    ps.setArray(2, array);

                    ps.setInt(3, queueItem.getReplyToMessageId());
                    ps.setString(4, queueItem.getTargetFormat().name());
                    ps.setInt(5, queueItem.getStatus().getCode());
                    if (queueItem.getExtra() == null) {
                        ps.setNull(6, Types.VARCHAR);
                    } else {
                        ps.setString(6, gson.toJson(queueItem.getExtra()));
                    }

                    return ps;
                },
                keyHolder
        );

        int id = ((Number) keyHolder.getKeys().get(ConversionQueueItem.ID)).intValue();
        queueItem.setId(id);
    }

    public Integer getQueuePosition(int id, SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COALESCE(queue_position, 1) as queue_position\n" +
                        "FROM (SELECT id, row_number() over (ORDER BY created_at) AS queue_position\n" +
                        "      FROM " + TYPE + " c\n" +
                        "      WHERE status = 0\n" +
                        "        AND files[1].format IN (" + inFormats() + ")\n" +
                        "        AND (SELECT sum(f.size) from unnest(c.files) f) " + getSign(weight) + " ?\n" +
                        ") as file_q\n" +
                        "WHERE id = ?",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
                },
                rs -> {
                    if (rs.next()) {
                        return rs.getInt(ConversionQueueItem.QUEUE_POSITION);
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

    @Override
    public List<ConversionQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH queue_items AS (\n" +
                        "    UPDATE " + TYPE + " SET " + QueueDao.POLL_UPDATE_LIST + " WHERE id IN (\n" +
                        "        SELECT id\n" +
                        "        FROM " + TYPE + " qu WHERE qu.status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n" +
                        " AND qu.files[1].format IN(" + inFormats() + ") " +
                        " AND NOT EXISTS(select 1 FROM " + DownloadQueueItem.NAME + " dq where dq.producer = '" + converter + "' AND dq.producer_id = qu.id AND dq.status != 3)\n" +
                        " AND total_files_to_download = (select COUNT(*) FROM " + DownloadQueueItem.NAME + " dq where dq.producer = '" + converter + "' AND dq.producer_id = qu.id)\n" +
                        QueueDao.POLL_ORDER_BY + "\n" +
                        "        LIMIT " + limit + ")\n" +
                        "    RETURNING *\n" +
                        ")\n" +
                        "SELECT cv.*, cc.files_json, 1 as queue_position, " +
                        "(SELECT json_agg(ds) FROM (SELECT * FROM " + DownloadQueueItem.NAME + " dq WHERE dq.producer = '" + converter + "' AND dq.producer_id = cv.id) as ds) as downloads\n" +
                        "FROM queue_items cv INNER JOIN (SELECT id, json_agg(files) as files_json FROM conversion_queue WHERE status = 0 GROUP BY id) cc ON cv.id = cc.id",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs, rowNum) -> map(rs)
        );
    }

    @Override
    public long countReadToComplete(SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COUNT(id) as cnt\n" +
                        "        FROM " + TYPE + " qu WHERE qu.status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n" +
                        " AND qu.files[1].format IN(" + inFormats() + ") " +
                        " AND NOT EXISTS(select 1 FROM " + DownloadQueueItem.NAME + " dq where dq.producer = '" + converter + "' AND dq.producer_id = qu.id AND dq.status != 3)\n" +
                        " AND total_files_to_download = (select COUNT(*) FROM " + DownloadQueueItem.NAME + " dq where dq.producer = '" + converter + "' AND dq.producer_id = qu.id)\n"
                ,
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs) -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    @Override
    public long countProcessing(SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COUNT(id) as cnt\n" +
                        "        FROM " + TYPE + " qu WHERE qu.status = 1 " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n" +
                        " AND qu.files[1].format IN(" + inFormats() + ")",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs) -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    public Long getTodayConversionsCount() {
        return jdbcTemplate.query(
                "SELECT count(*) as cnt FROM conversion_queue WHERE completed_at::date = current_date AND status = 3 AND files[1].format IN(" + inFormats() + ")",
                rs -> rs.next() ? rs.getLong("cnt") : -1
        );
    }

    public Long getYesterdayConversionsCount() {
        return jdbcTemplate.query(
                "SELECT count(*) as cnt FROM conversion_queue WHERE completed_at::date = current_date - interval '1 days' AND status = 3 " +
                        "AND files[1].format IN(" + inFormats() + ")",
                rs -> rs.next() ? rs.getLong("cnt") : -1
        );
    }

    public Long getAllConversionsCount() {
        return jdbcTemplate.query(
                "SELECT max(id) as cnt FROM conversion_queue WHERE status = 3",
                rs -> rs.next() ? rs.getLong("cnt") : -1
        );
    }

    public Long getTodayDailyActiveUsersCount() {
        return jdbcTemplate.query(
                "SELECT count(DISTINCT user_id) as cnt FROM conversion_queue WHERE completed_at::date = current_date AND status = 3 " +
                        "AND files[1].format IN(" + inFormats() + ")",
                rs -> rs.next() ? rs.getLong("cnt") : -1
        );
    }

    public void setResultFileId(int id, String fileId) {
        jdbcTemplate.update("UPDATE conversion_queue SET result_file_id = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, fileId);
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

    public void setFileId(int id, String fileId) {
        jdbcTemplate.update(
                "UPDATE conversion_queue SET files[1].file_id = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, fileId);
                    ps.setInt(2, id);
                }
        );
    }

    public ConversionQueueItem getById(int id) {
        SmartExecutorService.JobWeight weight = getWeight(id);

        if (weight == null) {
            return null;
        }
        return jdbcTemplate.query(
                "SELECT f.*, COALESCE(queue_place.queue_position, 1) as queue_position, cc.files_json\n" +
                        "FROM conversion_queue f\n" +
                        "         LEFT JOIN (SELECT id, row_number() over (ORDER BY created_at) as queue_position\n" +
                        "                     FROM conversion_queue c\n" +
                        "                     WHERE status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(c.files) f) " + getSign(weight) + " ?\n" +
                        " AND files[1].format IN (" + inFormats() + ")" +
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

    @Override
    public List<ConversionQueueItem> deleteAndGetProcessingOrWaitingByUserId(int userId) {
        return jdbcTemplate.query("WITH del AS(DELETE FROM conversion_queue WHERE user_id = ? " +
                        " AND files[1].format IN(" + inFormats() + ") " +
                        " AND status IN (0, 1) RETURNING id, status, files) SELECT id, status, json_agg(files) as files_json FROM del GROUP BY id, status",
                ps -> ps.setInt(1, userId),
                (rs, rowNum) -> {
                    ConversionQueueItem fileQueueItem = new ConversionQueueItem();

                    fileQueueItem.setId(rs.getInt(ConversionQueueItem.ID));
                    fileQueueItem.setStatus(ConversionQueueItem.Status.fromCode(rs.getInt(ConversionQueueItem.STATUS)));
                    fileQueueItem.setFiles(mapFiles(rs));

                    return fileQueueItem;
                }
        );
    }

    @Override
    public ConversionQueueItem deleteAndGetById(int id) {
        return jdbcTemplate.query(
                "WITH del AS(DELETE FROM conversion_queue WHERE id = ? RETURNING id, status, files) SELECT id, status, json_agg(files) as files_json FROM del GROUP BY id, status",
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

    public void setProgressMessageIdAndTotalFilesToDownload(int id, int progressMessageId, int totalFilesToDownload) {
        jdbcTemplate.update(
                "UPDATE conversion_queue SET progress_message_id = ?, total_files_to_download = ? where id = ?",
                ps -> {
                    ps.setInt(1, progressMessageId);
                    ps.setInt(2, totalFilesToDownload);
                    ps.setInt(3, id);
                }
        );
    }

    @Override
    public boolean isDeleteCompletedShouldBeDelegated() {
        return true;
    }

    @Override
    public List<ConversionQueueItem> deleteCompleted() {
        return jdbcTemplate.query(
                "WITH del AS(DELETE FROM conversion_queue qu WHERE qu.status = ? AND qu.completed_at + " + QueueDao.DELETE_COMPLETED_INTERVAL + " < now() " +
                        " AND files[1].format IN(" + inFormats() + ") " +
                        " AND NOT EXISTS(select true from " + ConversionReport.TYPE + " cr WHERE qu.id = cr.queue_item_id) RETURNING *)" +
                        "SELECT * FROM del",
                ps -> ps.setInt(1, QueueItem.Status.COMPLETED.getCode()),
                (rs, rowNum) -> map(rs)
        );
    }

    @Override
    public String getBaseAdditionalClause() {
        return "AND files[1].format IN(" + inFormats() + ")";
    }

    @Override
    public String getProducerName() {
        return converter;
    }

    @Override
    public String getQueueName() {
        return TYPE;
    }

    private String getSign(SmartExecutorService.JobWeight weight) {
        return weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">";
    }

    private ConversionQueueItem map(ResultSet rs) throws SQLException {
        Set<String> columns = JdbcUtils.getColumnNames(rs.getMetaData());
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setId(rs.getInt(ConversionQueueItem.ID));
        fileQueueItem.setReplyToMessageId(rs.getInt(ConversionQueueItem.REPLY_TO_MESSAGE_ID));
        fileQueueItem.setUserId(rs.getInt(ConversionQueueItem.USER_ID));
        fileQueueItem.setSuppressUserExceptions(rs.getBoolean(ConversionQueueItem.SUPPRESS_USER_EXCEPTIONS));
        fileQueueItem.setResultFileId(rs.getString(ConversionQueueItem.RESULT_FILE_ID));

        if (columns.contains(ConversionQueueItem.FILES_JSON)) {
            fileQueueItem.setFiles(mapFiles(rs));
        }

        Timestamp createdAt = rs.getTimestamp(ConversionQueueItem.CREATED_AT);
        if (createdAt != null) {
            fileQueueItem.setCreatedAt(ZonedDateTime.of(createdAt.toLocalDateTime(), ZoneOffset.UTC));
        }

        Timestamp startedAt = rs.getTimestamp(ConversionQueueItem.STARTED_AT);
        if (startedAt != null) {
            fileQueueItem.setStartedAt(ZonedDateTime.of(startedAt.toLocalDateTime(), ZoneOffset.UTC));
        }

        Timestamp completedAt = rs.getTimestamp(ConversionQueueItem.COMPLETED_AT);
        if (completedAt != null) {
            fileQueueItem.setCompletedAt(ZonedDateTime.of(completedAt.toLocalDateTime(), ZoneOffset.UTC));
        }

        Timestamp lastRunAt = rs.getTimestamp(ConversionQueueItem.LAST_RUN_AT);
        if (lastRunAt != null) {
            fileQueueItem.setLastRunAt(ZonedDateTime.of(lastRunAt.toLocalDateTime(), ZoneOffset.UTC));
        }

        fileQueueItem.setException(rs.getString(ConversionQueueItem.EXCEPTION));
        fileQueueItem.setTargetFormat(Format.valueOf(rs.getString(ConversionQueueItem.TARGET_FORMAT)));
        fileQueueItem.setMessage(rs.getString(ConversionQueueItem.MESSAGE));
        fileQueueItem.setProgressMessageId(rs.getInt(ConversionQueueItem.PROGRESS_MESSAGE_ID));

        fileQueueItem.setStatus(ConversionQueueItem.Status.fromCode(rs.getInt(ConversionQueueItem.STATUS)));
        if (columns.contains(ConversionQueueItem.QUEUE_POSITION)) {
            fileQueueItem.setQueuePosition(rs.getInt(ConversionQueueItem.QUEUE_POSITION));
        }

        if (columns.contains(ConversionQueueItem.EXTRA)) {
            fileQueueItem.setExtra(gson.fromJson(rs.getString(ConversionQueueItem.EXTRA), JsonElement.class));
        }

        if (columns.contains(ConversionQueueItem.DOWNLOADS)) {
            PGobject downloadsArr = (PGobject) rs.getObject(ConversionQueueItem.DOWNLOADS);
            if (downloadsArr != null) {
                try {
                    List<Map<String, Object>> values = objectMapper.readValue(downloadsArr.getValue(), new TypeReference<>() {
                    });
                    List<DownloadQueueItem> downloadingQueueItems = new ArrayList<>();
                    for (Map<String, Object> value : values) {
                        DownloadQueueItem downloadingQueueItem = new DownloadQueueItem();
                        downloadingQueueItem.setFilePath((String) value.get(DownloadQueueItem.FILE_PATH));
                        downloadingQueueItem.setFile(objectMapper.convertValue(value.get(DownloadQueueItem.FILE), TgFile.class));
                        downloadingQueueItem.setDeleteParentDir((Boolean) value.get(DownloadQueueItem.DELETE_PARENT_DIR));
                        downloadingQueueItems.add(downloadingQueueItem);
                    }
                    fileQueueItem.setDownloadQueueItems(downloadingQueueItems);
                } catch (JsonProcessingException e) {
                    throw new SQLException(e);
                }
            }
        }

        return fileQueueItem;
    }

    private List<TgFile> mapFiles(ResultSet rs) throws SQLException {
        PGobject jsonArr = (PGobject) rs.getObject(ConversionQueueItem.FILES_JSON);
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

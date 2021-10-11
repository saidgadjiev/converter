package ru.gadjini.telegram.converter.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.domain.ConversionReport;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.converter.utils.JdbcUtils;
import ru.gadjini.telegram.smart.bot.commons.dao.QueueDao;
import ru.gadjini.telegram.smart.bot.commons.dao.WorkQueueDaoDelegate;
import ru.gadjini.telegram.smart.bot.commons.domain.DownloadQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.property.ServerProperties;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.sql.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.converter.domain.ConversionQueueItem.TYPE;

@Repository
public class ConversionQueueDao implements WorkQueueDaoDelegate<ConversionQueueItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionQueueDao.class);

    private JdbcTemplate jdbcTemplate;

    private FileLimitProperties fileLimitProperties;

    private ServerProperties serverProperties;

    private Jackson jackson;

    private Set<String> converters;

    private ApplicationProperties applicationProperties;

    @Autowired
    public ConversionQueueDao(JdbcTemplate jdbcTemplate,
                              FileLimitProperties fileLimitProperties, ServerProperties serverProperties,
                              Jackson jackson, ApplicationProperties applicationProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileLimitProperties = fileLimitProperties;
        this.serverProperties = serverProperties;
        this.applicationProperties = applicationProperties;
        this.jackson = jackson;
        this.converters = applicationProperties.getConverters();
        LOGGER.debug("Light file weight({})", MemoryUtils.humanReadableByteCount(fileLimitProperties.getLightFileMaxWeight()));
    }

    public void create(ConversionQueueItem queueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO " + TYPE +
                            " (user_id, files, reply_to_message_id, target_format, status, extra, converter)\n" +
                            "    VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *", Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, queueItem.getUserId());

                    Object[] files = queueItem.getFiles().stream().map(TgFile::sqlObject).toArray();
                    Array array = con.createArrayOf(TgFile.TYPE, files);
                    ps.setArray(2, array);

                    ps.setInt(3, queueItem.getReplyToMessageId());
                    ps.setString(4, queueItem.getTargetFormat().name());
                    ps.setInt(5, queueItem.getStatus().getCode());
                    if (queueItem.getExtra() == null) {
                        ps.setNull(6, Types.VARCHAR);
                    } else {
                        ps.setString(6, jackson.writeValueAsString(queueItem.getExtra()));
                    }
                    ps.setString(7, queueItem.getConverter());

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
                        "        AND converter IN (" + inConverters() + ")\n" +
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

    public Integer countByUser(long userId) {
        return jdbcTemplate.query(
                "SELECT COUNT(*) as cnt FROM conversion_queue WHERE user_id = ? AND converter IN(" + inConverters() + ")",
                ps -> ps.setLong(1, userId),
                rs -> rs.next() ? rs.getInt("cnt") : 0
        );
    }

    public Long count(ConversionQueueItem.Status status) {
        return jdbcTemplate.query(
                "SELECT COUNT(*) as cnt FROM conversion_queue WHERE status = ? AND converter IN(" + inConverters() + ")",
                ps -> ps.setInt(1, status.getCode()),
                rs -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    @Override
    @Transactional
    public List<ConversionQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        String synchronizationColumn = DownloadQueueItem.getSynchronizationColumn(serverProperties.getNumber());

        return jdbcTemplate.query(
                "WITH queue_items AS (\n" +
                        "    UPDATE " + TYPE + " SET " + QueueDao.getUpdateList(serverProperties.getNumber()) + " WHERE id IN (\n" +
                        "        SELECT id\n" +
                        "        FROM " + TYPE + " qu WHERE qu.status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n" +
                        " AND qu.converter IN(" + inConverters() + ") " +
                        " AND NOT EXISTS(select 1 FROM " + DownloadQueueItem.NAME +
                        " dq where dq.producer = '" + applicationProperties.getConverter() + "' AND dq.producer_id = qu.id AND (dq.status != 3 OR dq." + synchronizationColumn + " = false))\n" +
                        " ORDER BY qu.id\n" +
                        " FOR UPDATE SKIP LOCKED LIMIT " + limit + ")\n" +
                        "    RETURNING *\n" +
                        ")\n" +
                        "SELECT cv.*, array_to_json(cv.files) as files_json, 1 as queue_position, " +
                        "(SELECT json_agg(ds) FROM (SELECT * FROM " + DownloadQueueItem.NAME + " dq " +
                        "WHERE dq.producer = '" + applicationProperties.getConverter() + "' AND dq.producer_id = cv.id) as ds) as downloads\n" +
                        "FROM queue_items cv",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs, rowNum) -> map(rs)
        );
    }

    @Override
    public long countReadToComplete(SmartExecutorService.JobWeight weight) {
        String synchronizationColumn = DownloadQueueItem.getSynchronizationColumn(serverProperties.getNumber());

        return jdbcTemplate.query(
                "SELECT COUNT(id) as cnt\n" +
                        "        FROM " + TYPE + " qu WHERE qu.status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n" +
                        " AND qu.converter IN(" + inConverters() + ") " +
                        " AND NOT EXISTS(select 1 FROM " + DownloadQueueItem.NAME + " dq where dq.producer = '" + applicationProperties.getConverter()
                        + "' AND dq.producer_id = qu.id AND (dq.status != 3 or " + synchronizationColumn + " = false))\n" +
                        " AND total_files_to_download = (select COUNT(*) FROM " + DownloadQueueItem.NAME +
                        " dq where dq.producer = '" + applicationProperties.getConverter() + "' AND dq.producer_id = qu.id)\n"
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
                        " AND qu.converter IN(" + inConverters() + ") AND server = " + serverProperties.getNumber(),
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs) -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    public Long getTodayConversionsCount() {
        return jdbcTemplate.query(
                "SELECT count(*) as cnt FROM conversion_queue WHERE completed_at::date = current_date " +
                        "AND status = 3 AND converter IN(" + inConverters() + ")",
                rs -> rs.next() ? rs.getLong("cnt") : -1
        );
    }

    public Long getYesterdayConversionsCount() {
        return jdbcTemplate.query(
                "SELECT count(*) as cnt FROM conversion_queue WHERE completed_at::date = current_date - interval '1 days' AND status = 3 " +
                        "AND converter IN(" + inConverters() + ")",
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
                        "AND converter IN(" + inConverters() + ")",
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
                "SELECT f.*, COALESCE(queue_place.queue_position, 1) as queue_position, array_to_json(f.files) as files_json\n" +
                        "FROM conversion_queue f\n" +
                        "         LEFT JOIN (SELECT id, row_number() over (ORDER BY created_at) as queue_position\n" +
                        "                     FROM conversion_queue c\n" +
                        "                     WHERE status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(c.files) f) " + getSign(weight) + " ?\n" +
                        " AND converter IN (" + inConverters() + ")" +
                        ") queue_place ON f.id = queue_place.id\n" +
                        "WHERE f.id = ?\n",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
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
    public ConversionQueueItem deleteAndGetProcessingOrWaitingById(int id) {
        return jdbcTemplate.query("DELETE FROM conversion_queue WHERE id = ? AND status IN (0, 1) RETURNING id, status, server",
                ps -> ps.setInt(1, id),
                (rs) -> {
                    if (rs.next()) {
                        return mapDeleteItem(rs);
                    }

                    return null;
                }
        );
    }

    @Override
    public List<ConversionQueueItem> deleteAndGetProcessingOrWaitingByUserId(long userId) {
        return jdbcTemplate.query("DELETE FROM conversion_queue WHERE user_id = ? " +
                        " AND converter IN(" + inConverters() + ") " +
                        " AND status IN (0, 1) RETURNING id, status, server",
                ps -> ps.setLong(1, userId),
                (rs, rowNum) -> mapDeleteItem(rs)
        );
    }

    @Override
    public ConversionQueueItem deleteAndGetById(int id) {
        return jdbcTemplate.query(
                "DELETE FROM conversion_queue WHERE id = ? RETURNING id, status, server",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        return mapDeleteItem(rs);
                    }

                    return null;
                }
        );
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        jdbcTemplate.update(
                "UPDATE conversion_queue SET progress_message_id = ? where id = ?",
                ps -> {
                    ps.setInt(1, progressMessageId);
                    ps.setInt(2, id);
                }
        );
    }

    public void setTotalFilesToDownload(int id, int totalFilesToDownload) {
        jdbcTemplate.update(
                "UPDATE conversion_queue SET total_files_to_download = ? where id = ?",
                ps -> {
                    ps.setInt(1, totalFilesToDownload);
                    ps.setInt(2, id);
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
                        " AND converter IN(" + inConverters() + ") " +
                        " AND NOT EXISTS(select true from " + ConversionReport.TYPE + " cr WHERE qu.id = cr.queue_item_id) RETURNING *)" +
                        "SELECT * FROM del",
                ps -> ps.setInt(1, QueueItem.Status.COMPLETED.getCode()),
                (rs, rowNum) -> map(rs)
        );
    }

    @Override
    public String getBaseAdditionalClause() {
        //language=SQL
        return "AND converter IN(" + inConverters() + ")";
    }

    @Override
    public String getProducerName() {
        return applicationProperties.getConverter();
    }

    @Override
    public String getQueueName() {
        return TYPE;
    }

    private String getSign(SmartExecutorService.JobWeight weight) {
        return weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">";
    }

    private ConversionQueueItem mapDeleteItem(ResultSet rs) throws SQLException {
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setId(rs.getInt(ConversionQueueItem.ID));
        fileQueueItem.setStatus(ConversionQueueItem.Status.fromCode(rs.getInt(ConversionQueueItem.STATUS)));
        fileQueueItem.setServer(rs.getInt(QueueItem.SERVER));

        return fileQueueItem;
    }

    private ConversionQueueItem map(ResultSet rs) throws SQLException {
        Set<String> columns = JdbcUtils.getColumnNames(rs.getMetaData());
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setId(rs.getInt(ConversionQueueItem.ID));
        fileQueueItem.setReplyToMessageId(rs.getInt(ConversionQueueItem.REPLY_TO_MESSAGE_ID));
        fileQueueItem.setUserId(rs.getInt(ConversionQueueItem.USER_ID));
        fileQueueItem.setSuppressUserExceptions(rs.getBoolean(ConversionQueueItem.SUPPRESS_USER_EXCEPTIONS));
        fileQueueItem.setResultFileId(rs.getString(ConversionQueueItem.RESULT_FILE_ID));
        fileQueueItem.setServer(rs.getInt(QueueItem.SERVER));
        if (columns.contains(ConversionQueueItem.TOTAL_FILES_TO_DOWNLOAD)) {
            fileQueueItem.setTotalFilesToDownload(rs.getLong(ConversionQueueItem.TOTAL_FILES_TO_DOWNLOAD));
        }

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
        fileQueueItem.setProgressMessageId(rs.getInt(ConversionQueueItem.PROGRESS_MESSAGE_ID));

        fileQueueItem.setStatus(ConversionQueueItem.Status.fromCode(rs.getInt(ConversionQueueItem.STATUS)));
        if (columns.contains(ConversionQueueItem.QUEUE_POSITION)) {
            fileQueueItem.setQueuePosition(rs.getInt(ConversionQueueItem.QUEUE_POSITION));
        }

        if (columns.contains(ConversionQueueItem.EXTRA)) {
            fileQueueItem.setExtra(jackson.readValue(rs.getString(ConversionQueueItem.EXTRA), JsonNode.class));
        }

        if (columns.contains(ConversionQueueItem.ATTEMPTS)) {
            fileQueueItem.setAttempts(rs.getInt(ConversionQueueItem.ATTEMPTS));
        }

        if (columns.contains(ConversionQueueItem.DOWNLOADS)) {
            PGobject downloadsArr = (PGobject) rs.getObject(ConversionQueueItem.DOWNLOADS);
            if (downloadsArr != null) {
                List<Map<String, Object>> values = jackson.readValue(downloadsArr.getValue(), new TypeReference<>() {
                });
                List<DownloadQueueItem> downloadingQueueItems = new ArrayList<>();
                for (Map<String, Object> value : values) {
                    DownloadQueueItem downloadingQueueItem = new DownloadQueueItem();
                    downloadingQueueItem.setFilePath((String) value.get(DownloadQueueItem.FILE_PATH));
                    downloadingQueueItem.setFile(jackson.convertValue(value.get(DownloadQueueItem.FILE), TgFile.class));
                    downloadingQueueItem.setDeleteParentDir((Boolean) value.get(DownloadQueueItem.DELETE_PARENT_DIR));
                    downloadingQueueItems.add(downloadingQueueItem);
                }
                fileQueueItem.setDownloadQueueItems(downloadingQueueItems);
            }
        }

        return fileQueueItem;
    }

    private List<TgFile> mapFiles(ResultSet rs) throws SQLException {
        PGobject jsonArr = (PGobject) rs.getObject(ConversionQueueItem.FILES_JSON);
        if (jsonArr != null) {
            return jackson.readValue(jsonArr.getValue(), new TypeReference<>() {
            });
        }

        return null;
    }

    private String inConverters() {
        return converters.stream().map(f -> "'" + f + "'").collect(Collectors.joining(", "));
    }
}

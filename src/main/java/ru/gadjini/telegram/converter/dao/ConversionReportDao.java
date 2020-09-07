package ru.gadjini.telegram.converter.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.domain.ConversionReport;

@Repository
public class ConversionReportDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ConversionReportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(ConversionReport fileReport) {
        jdbcTemplate.update(
                "INSERT INTO " + ConversionReport.TYPE + "(user_id, queue_item_id) VALUES (?, ?) ON CONFLICT (user_id, queue_item_id) DO NOTHING",
                ps -> {
                    ps.setInt(1, fileReport.getUserId());
                    ps.setInt(2, fileReport.getQueueItemId());
                }
        );
    }
}

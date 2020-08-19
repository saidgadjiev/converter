package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.dao.queue.ConversionReportDao;
import ru.gadjini.telegram.converter.domain.ConversionReport;

@Service
public class ConversinoReportService {

    private ConversionReportDao fileReportDao;

    @Autowired
    public ConversinoReportService(ConversionReportDao fileReportDao) {
        this.fileReportDao = fileReportDao;
    }

    public void createReport(int userId, int queueItemId) {
        ConversionReport fileReport = new ConversionReport();
        fileReport.setUserId(userId);
        fileReport.setQueueItemId(queueItemId);
        fileReportDao.create(fileReport);
    }
}

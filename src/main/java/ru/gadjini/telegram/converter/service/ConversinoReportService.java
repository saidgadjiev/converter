package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.dao.ConversionReportDao;
import ru.gadjini.telegram.converter.domain.ConversionReport;
import ru.gadjini.telegram.smart.bot.commons.dao.QueueDao;

@Service
public class ConversinoReportService {

    private ConversionReportDao fileReportDao;

    private QueueDao conversionQueueDao;

    @Autowired
    public ConversinoReportService(ConversionReportDao fileReportDao, QueueDao conversionQueueDao) {
        this.fileReportDao = fileReportDao;
        this.conversionQueueDao = conversionQueueDao;
    }

    public boolean createReport(int userId, int queueItemId) {
        if (conversionQueueDao.exists(queueItemId)) {
            ConversionReport fileReport = new ConversionReport();
            fileReport.setUserId(userId);
            fileReport.setQueueItemId(queueItemId);
            fileReportDao.create(fileReport);

            return true;
        }

        return false;
    }
}

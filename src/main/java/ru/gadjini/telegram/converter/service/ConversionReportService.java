package ru.gadjini.telegram.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.dao.ConversionReportDao;
import ru.gadjini.telegram.converter.domain.ConversionReport;

@Service
public class ConversionReportService {

    private ConversionReportDao conversionReportDao;

    @Autowired
    public ConversionReportService(ConversionReportDao conversionReportDao) {
        this.conversionReportDao = conversionReportDao;
    }

    public void createReport(long userId, int queueItemId) {
        ConversionReport fileReport = new ConversionReport();
        fileReport.setUserId(userId);
        fileReport.setQueueItemId(queueItemId);

        conversionReportDao.create(fileReport);
    }
}

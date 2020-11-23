package ru.gadjini.telegram.converter.service.queue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.dao.ConversionQueueDao;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.impl.ConvertState;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.Locale;

@Service
public class ConversionQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionQueueService.class);

    private ConversionQueueDao fileQueueDao;

    private LocalisationService localisationService;

    private FileLimitProperties fileLimitProperties;

    @Autowired
    public ConversionQueueService(ConversionQueueDao fileQueueDao, LocalisationService localisationService,
                                  FileLimitProperties fileLimitProperties) {
        this.fileQueueDao = fileQueueDao;
        this.localisationService = localisationService;
        this.fileLimitProperties = fileLimitProperties;
    }

    @Transactional
    public ConversionQueueItem create(User user, ConvertState convertState, Format targetFormat) {
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        convertState.getFiles().forEach(media -> {
            if (StringUtils.isBlank(media.getFileName())) {
                LOGGER.debug("Empty file name({}, {}, {})", user.getId(), targetFormat, convertState);
                media.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, new Locale(convertState.getUserLanguage())));
            }
            if (media.getFileSize() == 0) {
                LOGGER.warn("File size null({}, {}, {})", user.getId(), targetFormat, convertState);
            }
            fileQueueItem.addFile(media.toTgFile());
        });
        fileQueueItem.setUserId(user.getId());
        fileQueueItem.setReplyToMessageId(convertState.getMessageId());
        fileQueueItem.setStatus(ConversionQueueItem.Status.WAITING);
        fileQueueItem.setTargetFormat(targetFormat);

        fileQueueDao.create(fileQueueItem);
        fileQueueItem.setQueuePosition(fileQueueDao.getQueuePosition(fileQueueItem.getId(), fileQueueItem.getSize() > fileLimitProperties.getLightFileMaxWeight()
                ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT));

        return fileQueueItem;
    }

    public long count(ConversionQueueItem.Status status) {
        return fileQueueDao.count(status);
    }

    public void setResultFileId(int id, String fileId) {
        fileQueueDao.setResultFileId(id, fileId);
    }

    public void setFileId(int id, String fileId) {
        fileQueueDao.setFileId(id, fileId);
    }

    public long getTodayConversionsCount() {
        return fileQueueDao.getTodayConversionsCount();
    }

    public long getYesterdayConversionsCount() {
        return fileQueueDao.getYesterdayConversionsCount();
    }

    public long getWeeklyConversionsCount() {
        return fileQueueDao.getWeeklyConversionsCount();
    }

    public long getMonthlyConversionsCount() {
        return fileQueueDao.getMonthlyConversionsCount();
    }

    public long getAllConversionsCount() {
        return fileQueueDao.getAllConversionsCount();
    }

    public long getTodayDailyActiveUsersCount() {
        return fileQueueDao.getTodayDailyActiveUsersCount();
    }
}

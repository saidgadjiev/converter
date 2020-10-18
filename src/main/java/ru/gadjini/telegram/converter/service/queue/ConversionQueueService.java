package ru.gadjini.telegram.converter.service.queue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.dao.ConversionQueueDao;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.impl.ConvertState;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.User;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TimeCreator;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Locale;

@Service
public class ConversionQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionQueueService.class);

    private ConversionQueueDao fileQueueDao;

    private LocalisationService localisationService;

    private TimeCreator timeCreator;

    private FileLimitProperties fileLimitProperties;

    @Autowired
    public ConversionQueueService(ConversionQueueDao fileQueueDao, LocalisationService localisationService,
                                  TimeCreator timeCreator, FileLimitProperties fileLimitProperties) {
        this.fileQueueDao = fileQueueDao;
        this.localisationService = localisationService;
        this.timeCreator = timeCreator;
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

        fileQueueItem.setLastRunAt(timeCreator.now());
        fileQueueItem.setStatedAt(timeCreator.now());
        fileQueueDao.create(fileQueueItem);
        fileQueueItem.setPlaceInQueue(fileQueueDao.getPlaceInQueue(fileQueueItem.getId(), fileQueueItem.getSize() > fileLimitProperties.getLightFileMaxWeight()
                ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT));

        return fileQueueItem;
    }

    public long count(ConversionQueueItem.Status status) {
        return fileQueueDao.count(status);
    }

    @Transactional
    public List<ConversionQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return fileQueueDao.poll(weight, limit);
    }

    public void setWaiting(int id) {
        fileQueueDao.setWaiting(id);
    }

    public void setWaiting(int id, Throwable ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        fileQueueDao.setWaiting(id, exception);
    }

    public void setSuppressUserExceptions(int id, boolean suppressUserExceptions) {
        fileQueueDao.setSuppressUserExceptions(id, suppressUserExceptions);
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        fileQueueDao.setProgressMessageId(id, progressMessageId);
    }

    public void setResultFileId(int id, String fileId) {
        fileQueueDao.setResultFileId(id, fileId);
    }

    public void setFileId(int id, String fileId) {
        fileQueueDao.setFileId(id, fileId);
    }

    public String getException(int id) {
        return fileQueueDao.getException(id);
    }

    public void resetProcessing() {
        fileQueueDao.resetProcessing();
    }

    public ConversionQueueItem delete(int id) {
        return fileQueueDao.delete(id);
    }

    public void exceptionStatus(int id, Throwable ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        fileQueueDao.updateException(id, ConversionQueueItem.Status.EXCEPTION.getCode(), exception);
    }

    public void exception(int id, Throwable ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        fileQueueDao.updateException(id, exception);
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

    public void completeWithException(int id, String msg) {
        fileQueueDao.updateException(id, ConversionQueueItem.Status.COMPLETED.getCode(), msg);
    }

    public void converterNotFound(int id) {
        fileQueueDao.updateException(id, ConversionQueueItem.Status.CANDIDATE_NOT_FOUND.getCode(), "Converter not found");
    }

    public List<ConversionQueueItem> deleteProcessingOrWaitingByUserId(int userId) {
        return fileQueueDao.deleteProcessingOrWaitingByUserId(userId);
    }

    public void complete(int id) {
        fileQueueDao.updateCompletedAt(id, ConversionQueueItem.Status.COMPLETED.getCode());
    }

    public ConversionQueueItem getItem(int id) {
        return fileQueueDao.getById(id);
    }
}

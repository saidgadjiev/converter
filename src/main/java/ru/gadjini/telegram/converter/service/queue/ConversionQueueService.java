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

    @Autowired
    public ConversionQueueService(ConversionQueueDao fileQueueDao, LocalisationService localisationService, TimeCreator timeCreator) {
        this.fileQueueDao = fileQueueDao;
        this.localisationService = localisationService;
        this.timeCreator = timeCreator;
    }

    @Transactional
    public ConversionQueueItem createProcessingItem(User user, ConvertState convertState, Format targetFormat) {
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
        fileQueueItem.setStatus(ConversionQueueItem.Status.PROCESSING);
        fileQueueItem.setTargetFormat(targetFormat);

        fileQueueItem.setLastRunAt(timeCreator.now());
        fileQueueItem.setStatedAt(timeCreator.now());
        fileQueueDao.create(fileQueueItem);
        fileQueueItem.setPlaceInQueue(fileQueueDao.getPlaceInQueue(fileQueueItem.getId()));

        return fileQueueItem;
    }

    public ConversionQueueItem poll(SmartExecutorService.JobWeight weight) {
        List<ConversionQueueItem> poll = fileQueueDao.poll(weight, 1);

        return poll.isEmpty() ? null : poll.iterator().next();
    }

    @Transactional
    public List<ConversionQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return fileQueueDao.poll(weight, limit);
    }

    public void setWaiting(int id) {
        fileQueueDao.setWaiting(id);
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        fileQueueDao.setProgressMessageId(id, progressMessageId);
    }

    public void resetProcessing() {
        fileQueueDao.resetProcessing();
    }

    public ConversionQueueItem delete(int id) {
        return fileQueueDao.delete(id);
    }

    public void exception(int id, Exception ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        fileQueueDao.updateException(id, ConversionQueueItem.Status.EXCEPTION.getCode(), exception);
    }

    public void completeWithException(int id, String msg) {
        fileQueueDao.updateException(id, ConversionQueueItem.Status.COMPLETED.getCode(), msg);
    }

    public void converterNotFound(int id) {
        fileQueueDao.updateException(id, ConversionQueueItem.Status.CANDIDATE_NOT_FOUND.getCode(), "Converter not found");
    }

    public void complete(int id) {
        fileQueueDao.updateCompletedAt(id, ConversionQueueItem.Status.COMPLETED.getCode());
    }

    public List<ConversionQueueItem> getActiveItems(int userId) {
        return fileQueueDao.getActiveQueries(userId);
    }

    public ConversionQueueItem getItem(int id) {
        return fileQueueDao.getById(id);
    }

    public ConversionQueueItem poll(int id) {
        return fileQueueDao.poll(id);
    }
}

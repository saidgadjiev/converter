package ru.gadjini.telegram.converter.service.queue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.dao.ConversionQueueDao;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.Locale;

@Service
public class ConversionQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionQueueService.class);

    private ConversionQueueDao conversionQueueDao;

    private LocalisationService localisationService;

    private FileLimitProperties fileLimitProperties;

    private ApplicationProperties applicationProperties;

    @Autowired
    public ConversionQueueService(@Lazy ConversionQueueDao conversionQueueDao, LocalisationService localisationService,
                                  FileLimitProperties fileLimitProperties, ApplicationProperties applicationProperties) {
        this.conversionQueueDao = conversionQueueDao;
        this.localisationService = localisationService;
        this.fileLimitProperties = fileLimitProperties;
        this.applicationProperties = applicationProperties;
    }

    public ConversionQueueItem create(User user, ConvertState convertState, Format targetFormat) {
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setConverter(applicationProperties.getConverter());
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
        fileQueueItem.setExtra(convertState.getSettings());
        fileQueueItem.setTargetFormat(targetFormat);

        conversionQueueDao.create(fileQueueItem);
        fileQueueItem.setQueuePosition(conversionQueueDao.getQueuePosition(fileQueueItem.getId(), fileQueueItem.getSize() > fileLimitProperties.getLightFileMaxWeight()
                ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT));

        return fileQueueItem;
    }

    public int countByUser(long userId) {
        return conversionQueueDao.countByUser(userId);
    }

    public long count(ConversionQueueItem.Status status) {
        return conversionQueueDao.count(status);
    }

    public void setResultFileId(int id, String fileId) {
        conversionQueueDao.setResultFileId(id, fileId);
    }

    public void setFileId(int id, String fileId) {
        conversionQueueDao.setFileId(id, fileId);
    }

    public long getTodayConversionsCount() {
        return conversionQueueDao.getTodayConversionsCount();
    }

    public long getYesterdayConversionsCount() {
        return conversionQueueDao.getYesterdayConversionsCount();
    }

    public long getAllConversionsCount() {
        return conversionQueueDao.getAllConversionsCount();
    }

    public long getTodayDailyActiveUsersCount() {
        return conversionQueueDao.getTodayDailyActiveUsersCount();
    }

    public void setProgressMessageIdAndTotalFilesToDownload(int id, int messageId, int totalFilesToDownload) {
        conversionQueueDao.setProgressMessageIdAndTotalFilesToDownload(id, messageId, totalFilesToDownload);
    }
}

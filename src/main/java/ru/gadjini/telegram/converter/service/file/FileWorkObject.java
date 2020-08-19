package ru.gadjini.telegram.converter.service.file;

import ru.gadjini.telegram.converter.service.message.TelegramMediaServiceProvider;

import java.util.concurrent.TimeUnit;

public class FileWorkObject {

    private static final int TTL = 3 * 60;

    private long chatId;

    private long fileSize;

    private FileLimitsDao fileLimitsDao;

    private TelegramMediaServiceProvider mediaServiceProvider;

    public FileWorkObject(long chatId, long fileSize, FileLimitsDao fileLimitsDao, TelegramMediaServiceProvider mediaServiceProvider) {
        this.chatId = chatId;
        this.fileSize = fileSize;
        this.fileLimitsDao = fileLimitsDao;
        this.mediaServiceProvider = mediaServiceProvider;
    }

    public long getChatId() {
        return chatId;
    }

    public void start() {
        if (mediaServiceProvider.isBotApiDownloadFile(fileSize)) {
            return;
        }
        fileLimitsDao.setState(chatId, InputFileState.State.PROCESSING);
    }

    public void stop() {
        if (mediaServiceProvider.isBotApiDownloadFile(fileSize)) {
            return;
        }
        fileLimitsDao.setState(chatId, InputFileState.State.COMPLETED);
        fileLimitsDao.setInputFileTtl(chatId, TTL, TimeUnit.SECONDS);
    }
}

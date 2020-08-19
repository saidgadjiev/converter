package ru.gadjini.telegram.converter.service.message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.model.bot.api.object.InputFile;
import ru.gadjini.telegram.converter.model.bot.api.object.InputMedia;
import ru.gadjini.telegram.converter.service.telegram.TelegramBotApiService;
import ru.gadjini.telegram.converter.service.telegram.TelegramMTProtoService;
import ru.gadjini.telegram.converter.service.telegram.TelegramMediaService;

import java.io.File;

import static ru.gadjini.telegram.converter.common.TgConstants.BOT_API_DOWNLOAD_FILE_LIMIT;
import static ru.gadjini.telegram.converter.common.TgConstants.BOT_API_UPLOAD_FILE_LIMIT;

@Component
public class TelegramMediaServiceProvider {

    private TelegramBotApiService telegramBotApiService;

    private TelegramMTProtoService telegramMTProtoService;

    @Autowired
    public TelegramMediaServiceProvider(TelegramBotApiService telegramBotApiService, TelegramMTProtoService telegramMTProtoService) {
        this.telegramBotApiService = telegramBotApiService;
        this.telegramMTProtoService = telegramMTProtoService;
    }

    public boolean isBotApiDownloadFile(long fileSize) {
        return fileSize > 0 && fileSize <= BOT_API_DOWNLOAD_FILE_LIMIT;
    }

    public boolean isBotApiUploadFile(long fileSize) {
        return fileSize > 0 && fileSize <= BOT_API_UPLOAD_FILE_LIMIT;
    }

    public TelegramMediaService getMediaService(InputMedia media) {
        return getMediaService(media.getFileId(), media.getFilePath());
    }

    public TelegramMediaService getMediaService(InputFile media) {
        return getMediaService(media.getFileId(), media.getFilePath());
    }

    public TelegramMediaService getStickerMediaService() {
        return telegramBotApiService;
    }

    public TelegramMediaService getMediaService(String fileId, String filePath) {
        if (StringUtils.isNotBlank(fileId)) {
            return telegramBotApiService;
        }
        File file = new File(filePath);

        if (isBotApiUploadFile(file.length())) {
            return telegramBotApiService;
        }

        return telegramMTProtoService;
    }

    public TelegramMediaService getDownloadMediaService(long fileSize) {
        if (isBotApiDownloadFile(fileSize)) {
            return telegramBotApiService;
        }

        return telegramMTProtoService;
    }
}

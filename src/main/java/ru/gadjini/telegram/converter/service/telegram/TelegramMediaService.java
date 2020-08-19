package ru.gadjini.telegram.converter.service.telegram;

import ru.gadjini.telegram.converter.io.SmartTempFile;
import ru.gadjini.telegram.converter.model.bot.api.method.send.*;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.Progress;

public interface TelegramMediaService {
    Message editMessageMedia(EditMessageMedia editMessageMedia);

    Message sendSticker(SendSticker sendSticker);

    Message sendDocument(SendDocument sendDocument);

    Message sendVideo(SendVideo sendVideo);

    Message sendAudio(SendAudio sendAudio);

    Message sendPhoto(SendPhoto sendPhoto);

    void downloadFileByFileId(String fileId, long fileSize, SmartTempFile outputFile);

    void downloadFileByFileId(String fileId, long fileSize, Progress progress, SmartTempFile outputFile);

    boolean cancelUploading(String filePath);

    boolean cancelDownloading(String fileId);

    void cancelDownloads();
}

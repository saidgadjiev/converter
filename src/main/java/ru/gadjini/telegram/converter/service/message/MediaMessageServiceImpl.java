package ru.gadjini.telegram.converter.service.message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.model.EditMediaResult;
import ru.gadjini.telegram.converter.model.SendFileResult;
import ru.gadjini.telegram.converter.model.bot.api.MediaType;
import ru.gadjini.telegram.converter.model.bot.api.method.send.*;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.ParseMode;
import ru.gadjini.telegram.converter.service.FileService;
import ru.gadjini.telegram.converter.service.telegram.TelegramMTProtoService;

@Service
@Qualifier("media")
public class MediaMessageServiceImpl implements MediaMessageService {

    private FileService fileService;

    private TelegramMTProtoService telegramService;

    private TelegramMediaServiceProvider mediaServiceProvider;

    @Autowired
    public MediaMessageServiceImpl(FileService fileService,
                                   TelegramMTProtoService telegramService, TelegramMediaServiceProvider mediaServiceProvider) {
        this.fileService = fileService;
        this.telegramService = telegramService;
        this.mediaServiceProvider = mediaServiceProvider;
    }


    @Override
    public EditMediaResult editMessageMedia(EditMessageMedia editMessageMedia) {
        if (StringUtils.isNotBlank(editMessageMedia.getMedia().getCaption())) {
            editMessageMedia.getMedia().setParseMode(ParseMode.HTML);
        }
        Message message = mediaServiceProvider.getMediaService(editMessageMedia.getMedia()).editMessageMedia(editMessageMedia);

        return new EditMediaResult(fileService.getFileId(message));
    }

    @Override
    public SendFileResult sendDocument(SendDocument sendDocument) {
        if (StringUtils.isNotBlank(sendDocument.getCaption())) {
            sendDocument.setParseMode(ParseMode.HTML);
        }

        Message message = mediaServiceProvider.getMediaService(sendDocument.getDocument()).sendDocument(sendDocument);

        return new SendFileResult(message.getMessageId(), fileService.getFileId(message));
    }

    @Override
    public SendFileResult sendPhoto(SendPhoto sendPhoto) {
        Message message = mediaServiceProvider.getMediaService(sendPhoto.getPhoto()).sendPhoto(sendPhoto);

        return new SendFileResult(message.getMessageId(), fileService.getFileId(message));
    }

    @Override
    public void sendVideo(SendVideo sendVideo) {
        if (StringUtils.isNotBlank(sendVideo.getCaption())) {
            sendVideo.setParseMode(ParseMode.HTML);
        }
        mediaServiceProvider.getMediaService(sendVideo.getVideo()).sendVideo(sendVideo);
    }

    @Override
    public void sendAudio(SendAudio sendAudio) {
        if (StringUtils.isNotBlank(sendAudio.getCaption())) {
            sendAudio.setParseMode(ParseMode.HTML);
        }

        mediaServiceProvider.getMediaService(sendAudio.getAudio()).sendAudio(sendAudio);
    }

    @Override
    public MediaType getMediaType(String fileId) {
        return telegramService.getMediaType(fileId);
    }

    @Override
    public void sendFile(long chatId, String fileId) {
        sendFile(chatId, fileId, null);
    }

    @Override
    public void sendFile(long chatId, String fileId, String caption) {
        MediaType mediaType = getMediaType(fileId);

        switch (mediaType) {
            case PHOTO:
                sendPhoto(new SendPhoto(chatId, fileId).setCaption(caption));
                break;
            case VIDEO:
                sendVideo(new SendVideo(chatId, fileId).setCaption(caption));
                break;
            case AUDIO:
                sendAudio(new SendAudio(chatId, fileId).setCaption(caption));
                break;
            default:
                sendDocument(new SendDocument(chatId, fileId).setCaption(caption));
                break;
        }
    }

    @Override
    public void sendSticker(SendSticker sendSticker) {
        mediaServiceProvider.getStickerMediaService().sendSticker(sendSticker);
    }
}

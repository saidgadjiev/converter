package ru.gadjini.telegram.converter.service.message;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.common.TgConstants;
import ru.gadjini.telegram.converter.model.EditMediaResult;
import ru.gadjini.telegram.converter.model.SendFileResult;
import ru.gadjini.telegram.converter.model.bot.api.MediaType;
import ru.gadjini.telegram.converter.model.bot.api.method.send.*;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.telegram.converter.model.bot.api.object.InputFile;
import ru.gadjini.telegram.converter.service.LocalisationService;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.utils.MemoryUtils;

import java.io.File;
import java.util.Arrays;

@Component
@Qualifier("medialimits")
public class TgLimitsMediaMessageService implements MediaMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TgLimitsMediaMessageService.class);

    private UserService userService;

    private MediaMessageService mediaMessageService;

    private MessageService messageService;

    private LocalisationService localisationService;

    @Autowired
    public TgLimitsMediaMessageService(UserService userService, LocalisationService localisationService) {
        this.userService = userService;
        this.localisationService = localisationService;
    }

    @Autowired
    public void setMediaMessageService(@Qualifier("media") MediaMessageService mediaMessageService) {
        this.mediaMessageService = mediaMessageService;
    }

    @Autowired
    public void setMediaMessageService(@Qualifier("messagelimits") MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public EditMediaResult editMessageMedia(EditMessageMedia editMediaContext) {
        return mediaMessageService.editMessageMedia(editMediaContext);
    }

    @Override
    public void sendSticker(SendSticker sendSticker) {
        mediaMessageService.sendSticker(sendSticker);
    }

    @Override
    public SendFileResult sendDocument(SendDocument sendDocument) {
        if (validate(sendDocument)) {
            return mediaMessageService.sendDocument(sendDocument);
        }

        return null;
    }

    @Override
    public SendFileResult sendPhoto(SendPhoto sendPhoto) {
        return mediaMessageService.sendPhoto(sendPhoto);
    }

    @Override
    public void sendVideo(SendVideo sendVideo) {
        mediaMessageService.sendVideo(sendVideo);
    }

    @Override
    public void sendAudio(SendAudio sendAudio) {
        mediaMessageService.sendAudio(sendAudio);
    }

    @Override
    public MediaType getMediaType(String fileId) {
        return mediaMessageService.getMediaType(fileId);
    }

    @Override
    public void sendFile(long chatId, String fileId) {
        mediaMessageService.sendFile(chatId, fileId);
    }

    @Override
    public void sendFile(long chatId, String fileId, String caption) {
        mediaMessageService.sendFile(chatId, fileId, caption);
    }

    private boolean validate(SendDocument sendDocument) {
        InputFile document = sendDocument.getDocument();
        if (StringUtils.isNotBlank(document.getFileId())) {
            return true;
        }
        File file = new File(document.getFilePath());
        if (file.length() == 0) {
            LOGGER.error("Zero file\n{}", Arrays.toString(Thread.currentThread().getStackTrace()));
            messageService.sendMessage(new SendMessage(sendDocument.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ZERO_LENGTH_FILE, userService.getLocaleOrDefault((int) sendDocument.getOrigChatId())))
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        }
        if (file.length() > TgConstants.LARGE_FILE_SIZE) {
            LOGGER.debug("Large out file({}, {})", sendDocument.getChatId(), MemoryUtils.humanReadableByteCount(file.length()));
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_TOO_LARGE_OUT_FILE,
                    new Object[]{file.getName(), MemoryUtils.humanReadableByteCount(file.length())},
                    userService.getLocaleOrDefault((int) sendDocument.getOrigChatId()));

            messageService.sendMessage(new SendMessage(sendDocument.getChatId(), text)
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        } else {
            return true;
        }
    }
}

package ru.gadjini.telegram.converter.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.model.Any2AnyFile;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.PhotoSize;
import ru.gadjini.telegram.converter.model.bot.api.object.Sticker;
import ru.gadjini.telegram.converter.service.conversion.api.Format;
import ru.gadjini.telegram.converter.service.conversion.impl.FormatService;

import java.util.Comparator;
import java.util.Locale;

@Service
public class FileService {

    private LocalisationService localisationService;

    private FormatService formatService;

    @Autowired
    public FileService(LocalisationService localisationService, FormatService formatService) {
        this.localisationService = localisationService;
        this.formatService = formatService;
    }

    public String getFileId(Message message) {
        if (message.hasDocument()) {
            return message.getDocument().getFileId();
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();

            return photoSize.getFileId();
        } else if (message.hasVideo()) {
            return message.getVideo().getFileId();
        } else if (message.hasAudio()) {
            return message.getAudio().getFileId();
        } else if (message.hasSticker()) {
            Sticker sticker = message.getSticker();

            return sticker.getFileId();
        }

        return null;
    }

    public Any2AnyFile getFile(Message message, Locale locale) {
        Any2AnyFile any2AnyFile = new Any2AnyFile();

        if (message.hasDocument()) {
            any2AnyFile.setFileName(message.getDocument().getFileName());
            any2AnyFile.setFileId(message.getDocument().getFileId());
            any2AnyFile.setMimeType(message.getDocument().getMimeType());
            if (message.getDocument().getFileSize() != null) {
                any2AnyFile.setFileSize(message.getDocument().getFileSize());
            }
            any2AnyFile.setThumb(message.getDocument().hasThumb() ? message.getDocument().getThumb().getFileId() : null);
            any2AnyFile.setFormat(formatService.getFormat(any2AnyFile.getFileName(), any2AnyFile.getMimeType()));

            return any2AnyFile;
        } else if (message.hasPhoto()) {
            any2AnyFile.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".jpg");
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            any2AnyFile.setFileId(photoSize.getFileId());
            any2AnyFile.setMimeType("image/jpeg");
            if (photoSize.getFileSize() != null) {
                any2AnyFile.setFileSize(photoSize.getFileSize());
            }
            any2AnyFile.setFormat(Format.JPG);

            return any2AnyFile;
        } else if (message.hasVideo()) {
            String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".";
            Format format = formatService.getFormat(message.getVideo().getFileName(), message.getVideo().getMimeType());
            if (format != null) {
                fileName += format.getExt();
            } else {
                fileName += "mp4";
            }
            any2AnyFile.setFileName(fileName);
            any2AnyFile.setFileId(message.getVideo().getFileId());
            if (message.getVideo().getFileSize() != null) {
                any2AnyFile.setFileSize(message.getVideo().getFileSize());
            }
            any2AnyFile.setFileName(message.getVideo().getFileName());
            any2AnyFile.setThumb(message.getVideo().hasThumb() ? message.getVideo().getThumb().getFileId() : null);
            any2AnyFile.setMimeType(message.getVideo().getMimeType());
            any2AnyFile.setFormat(format);

            return any2AnyFile;
        } else if (message.hasAudio()) {
            String fileName = message.getAudio().getFileName();
            Format format = formatService.getFormat(message.getAudio().getFileName(), message.getAudio().getMimeType());

            if (StringUtils.isBlank(fileName)) {
                fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".";
                fileName += format.getExt();
            }
            any2AnyFile.setFileName(fileName);
            any2AnyFile.setFileId(message.getAudio().getFileId());
            any2AnyFile.setMimeType(message.getAudio().getMimeType());
            if (message.getAudio().getFileSize() != null) {
                any2AnyFile.setFileSize(message.getAudio().getFileSize());
            }
            any2AnyFile.setFileName(message.getAudio().getFileName());
            any2AnyFile.setThumb(message.getAudio().hasThumb() ? message.getAudio().getThumb().getFileId() : null);
            any2AnyFile.setFormat(format);

            return any2AnyFile;
        } else if (message.hasSticker()) {
            Sticker sticker = message.getSticker();
            any2AnyFile.setFileId(sticker.getFileId());
            String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".";
            fileName += sticker.getAnimated() ? "tgs" : "webp";
            any2AnyFile.setFileName(fileName);
            any2AnyFile.setMimeType(sticker.getAnimated() ? null : "image/webp");
            if (message.getSticker().getFileSize() != null) {
                any2AnyFile.setFileSize(message.getSticker().getFileSize());
            }
            any2AnyFile.setFormat(sticker.getAnimated() ? Format.TGS : Format.WEBP);

            return any2AnyFile;
        }

        return null;
    }
}

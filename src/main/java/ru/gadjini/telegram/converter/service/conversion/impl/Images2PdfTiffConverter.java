package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.Image2PdfDevice;
import ru.gadjini.telegram.converter.service.image.device.ImageMagickDevice;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.io.File;
import java.util.*;

import static ru.gadjini.telegram.converter.common.MessagesProperties.MESSAGE_CALCULATED;

@Component
public class Images2PdfTiffConverter extends BaseAny2AnyConverter {

    private static final String TAG = "images2pdftiff";

    private static final Logger LOGGER = LoggerFactory.getLogger(Images2PdfTiffConverter.class);

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGES), List.of(Format.PDF, Format.TIFF)
    );

    private TempFileService fileService;

    private FileManager fileManager;

    private ImageMagickDevice magickDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private MessageService messageService;

    private Image2PdfDevice image2PdfDevice;

    private ConversionMessageBuilder messageBuilder;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public Images2PdfTiffConverter(TempFileService fileService, FileManager fileManager,
                                   ImageMagickDevice magickDevice,
                                   LocalisationService localisationService, UserService userService,
                                   @Qualifier("messageLimits") MessageService messageService,
                                   Image2PdfDevice image2PdfDevice, ConversionMessageBuilder messageBuilder,
                                   InlineKeyboardService inlineKeyboardService) {
        super(MAP);
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.magickDevice = magickDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.messageService = messageService;
        this.image2PdfDevice = image2PdfDevice;
        this.messageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
        List<SmartTempFile> images = downloadImages(fileQueueItem, locale);

        try {
            String parentDir = images.iterator().next().getParent() + File.separator;
            magickDevice.changeFormatAndRemoveAlphaChannel(parentDir + "*", Format.PNG.getExt());

            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale);
                if (fileQueueItem.getTargetFormat() == Format.PDF) {
                    image2PdfDevice.convert2Pdf(parentDir + "*.png", result.getAbsolutePath(), fileName);
                } else {
                    magickDevice.convert2Tiff(parentDir + "*.png", result.getAbsolutePath());
                }

                return new FileResult(fileName + "." + fileQueueItem.getTargetFormat().getExt(), result);
            } catch (Throwable e) {
                result.smartDelete();
                throw e;
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            images.forEach(SmartTempFile::smartDelete);
        }
    }

    private List<SmartTempFile> downloadImages(ConversionQueueItem queueItem, Locale locale) {
        List<SmartTempFile> images = new ArrayList<>();
        SmartTempFile tempDir = fileService.createTempDir(queueItem.getUserId(), TAG);

        downloadingStartProgress(queueItem, locale);
        try {
            int i = 0;
            for (TgFile imageFile : queueItem.getFiles()) {
                SmartTempFile downloadedImage = fileService.createTempFile(tempDir, queueItem.getUserId(), "File-" + i + "." + imageFile.getFormat().getExt());
                images.add(downloadedImage);
                Progress downloadProgress = progress(queueItem, i, queueItem.getFiles().size(), locale);
                fileManager.downloadFileByFileId(imageFile.getFileId(), imageFile.getSize(), downloadProgress, downloadedImage);
                ++i;
            }
        } catch (Exception ex) {
            images.forEach(SmartTempFile::smartDelete);
            tempDir.smartDelete();
            throw ex;
        }
        downloadingFinishedProgress(queueItem, locale);

        return images;
    }

    private Progress progress(ConversionQueueItem queueItem, int current, int total, Locale locale) {
        Progress progress = new Progress();
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setLocale(locale.getLanguage());
        progress.setProgressMessage(messageBuilder.getFilesDownloadingProgressMessage(queueItem, queueItem.getFiles().get(current).getSize(), current, total, Lang.PYTHON, locale));
        progress.setProgressReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));

        if (current + 1 < total) {
            String completionMessage = messageBuilder.getFilesDownloadingProgressMessage(queueItem, queueItem.getFiles().get(current + 1).getSize(), current + 1, total, Lang.JAVA, locale);
            String calculated = localisationService.getMessage(MESSAGE_CALCULATED, locale);
            completionMessage = String.format(completionMessage, 0, calculated, calculated);
            progress.setAfterProgressCompletionMessage(completionMessage);
            progress.setProgressReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));
        } else {
            String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, 0, Collections.emptySet(),
                    ConversionStep.CONVERTING, Lang.JAVA, locale);
            progress.setAfterProgressCompletionMessage(completionMessage);
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale));
        }

        return progress;
    }

    private void downloadingFinishedProgress(ConversionQueueItem queueItem, Locale locale) {
        long lastFileSize = queueItem.getFiles().get(queueItem.getFiles().size() - 1).getSize();
        if (!isShowingProgress(lastFileSize)) {
            try {
                String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, 1, Collections.emptySet(),
                        ConversionStep.CONVERTING, Lang.JAVA, locale);
                InlineKeyboardMarkup conversionKeyboard = inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale);
                messageService.editMessage(new EditMessageText(queueItem.getUserId(), queueItem.getProgressMessageId(), progressMessage)
                        .setReplyMarkup(conversionKeyboard));
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    private void downloadingStartProgress(ConversionQueueItem queueItem, Locale locale) {
        long firstFileSize = queueItem.getFiles().iterator().next().getSize();
        if (!isShowingProgress(firstFileSize)) {
            try {
                String progressMessage = messageBuilder.getFilesDownloadingProgressMessage(queueItem, 1, 0, queueItem.getFiles().size(), Lang.JAVA, locale);
                InlineKeyboardMarkup conversionKeyboard = inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale);
                messageService.editMessage(new EditMessageText(queueItem.getUserId(), queueItem.getProgressMessageId(), progressMessage)
                        .setReplyMarkup(conversionKeyboard));
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    private boolean isShowingProgress(long fileSize) {
        return fileSize > 5 * 1024 * 1024;
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.ImageConvertDevice;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.service.progress.Lang;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class Images2PdfTiffConverter extends BaseAny2AnyConverter {

    private static final String TAG = "images2pdftiff";

    private static final Logger LOGGER = LoggerFactory.getLogger(Images2PdfTiffConverter.class);

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGES), List.of(Format.PDF, Format.TIFF)
    );

    private TempFileService fileService;

    private FileManager fileManager;

    private ImageConvertDevice imageConvertDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private MessageService messageService;

    private ConversionMessageBuilder messageBuilder;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public Images2PdfTiffConverter(TempFileService fileService, FileManager fileManager,
                                   ImageConvertDevice imageConvertDevice,
                                   LocalisationService localisationService, UserService userService,
                                   @Qualifier("messageLimits") MessageService messageService,
                                   ConversionMessageBuilder messageBuilder, InlineKeyboardService inlineKeyboardService) {
        super(MAP);
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.imageConvertDevice = imageConvertDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
        List<SmartTempFile> images = downloadImages(fileQueueItem, locale);

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String parentDir = images.iterator().next().getParent() + File.separator;
            imageConvertDevice.convert2Format(parentDir + "*", Format.PNG.getExt());

            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), TAG, fileQueueItem.getTargetFormat().getExt());

            String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale);
            if (fileQueueItem.getTargetFormat() == Format.PDF) {
                imageConvertDevice.convert2Pdf(parentDir + "*.png", result.getAbsolutePath(), fileName);
            } else {
                imageConvertDevice.convertImages(parentDir + "*.png", result.getAbsolutePath());
            }

            stopWatch.stop();

            return new FileResult(fileName + "." + fileQueueItem.getTargetFormat().getExt(), result, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            images.forEach(SmartTempFile::smartDelete);
        }
    }

    private List<SmartTempFile> downloadImages(ConversionQueueItem queueItem, Locale locale) {
        List<SmartTempFile> images = new ArrayList<>();
        SmartTempFile tempDir = fileService.createTempDir(queueItem.getUserId(), TAG);

        try {
            try {
                String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, 0, Collections.emptySet(),
                        ConversionStep.DOWNLOADING, Lang.JAVA, locale);
                InlineKeyboardMarkup conversionKeyboard = inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale);
                messageService.editMessage(new EditMessageText(queueItem.getUserId(), queueItem.getProgressMessageId(), progressMessage)
                        .setReplyMarkup(conversionKeyboard));
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            int i = 1;
            for (TgFile imageFile : queueItem.getFiles()) {
                SmartTempFile downloadedImage = fileService.createTempFile(tempDir, queueItem.getUserId(), "File-" + i++ + "." + imageFile.getFormat().getExt());
                images.add(downloadedImage);
                fileManager.downloadFileByFileId(imageFile.getFileId(), imageFile.getSize(), downloadedImage);
            }
        } catch (Exception ex) {
            images.forEach(SmartTempFile::smartDelete);
            tempDir.smartDelete();
            throw ex;
        }
        try {
            String progressMessage = messageBuilder.getConversionProcessingMessage(queueItem, 0, Collections.emptySet(),
                    ConversionStep.CONVERTING, Lang.JAVA, locale);
            InlineKeyboardMarkup conversionKeyboard = inlineKeyboardService.getConversionKeyboard(queueItem.getId(), locale);
            messageService.editMessage(new EditMessageText(queueItem.getUserId(), queueItem.getProgressMessageId(), progressMessage)
                    .setReplyMarkup(conversionKeyboard));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        return images;
    }
}

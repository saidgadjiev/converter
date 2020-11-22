package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.Image2PdfDevice;
import ru.gadjini.telegram.converter.service.image.device.ImageMagickDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.converter.common.MessagesProperties.MESSAGE_CALCULATED;

@Component
public class Images2PdfTiffConverter extends BaseAny2AnyConverter {

    private static final String TAG = "images2pdftiff";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGES), List.of(Format.PDF, Format.TIFF)
    );

    private TempFileService fileService;

    private ImageMagickDevice magickDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private Image2PdfDevice image2PdfDevice;

    private ConversionMessageBuilder messageBuilder;

    private SmartInlineKeyboardService inlineKeyboardService;

    private FileDownloadService fileDownloadService;

    @Autowired
    public Images2PdfTiffConverter(TempFileService fileService,
                                   ImageMagickDevice magickDevice,
                                   LocalisationService localisationService, UserService userService,
                                   Image2PdfDevice image2PdfDevice, ConversionMessageBuilder messageBuilder,
                                   SmartInlineKeyboardService inlineKeyboardService, FileDownloadService fileDownloadService) {
        super(MAP);
        this.fileService = fileService;
        this.magickDevice = magickDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.image2PdfDevice = image2PdfDevice;
        this.messageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
        this.fileDownloadService = fileDownloadService;
    }

    @Override
    public void createDownloads(ConversionQueueItem conversionQueueItem) {
        Collection<TgFile> tgFiles = prepareFilesToDownload(conversionQueueItem);
        fileDownloadService.createDownloads(tgFiles, conversionQueueItem.getId());
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        return doConvert(fileQueueItem, fileQueueItem.getTargetFormat());
    }

    public ConvertResult doConvert(ConversionQueueItem fileQueueItem, Format targetFormat) {
        Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
        List<SmartTempFile> images = fileQueueItem.getDownloadedFiles()
                .stream()
                .map(downloadingQueueItem -> new SmartTempFile(new File(downloadingQueueItem.getFilePath()), downloadingQueueItem.isDeleteParentDir()))
                .collect(Collectors.toList());

        try {
            String parentDir = images.iterator().next().getParent() + File.separator;
            magickDevice.changeFormatAndRemoveAlphaChannel(parentDir + "*", Format.PNG.getExt());

            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), TAG, targetFormat.getExt());
            try {
                String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale);
                if (targetFormat == Format.PDF) {
                    image2PdfDevice.convert2Pdf(parentDir + "*.png", result.getAbsolutePath(), fileName);
                } else {
                    magickDevice.convert2Tiff(parentDir + "*.png", result.getAbsolutePath());
                }

                return new FileResult(fileName + "." + targetFormat.getExt(), result, null);
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

    private Collection<TgFile> prepareFilesToDownload(ConversionQueueItem queueItem) {
        Collection<TgFile> tgFiles = queueItem.getFiles();
        String tempDir = fileService.getTempDir(queueItem.getUserId(), TAG);
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        int i = 0;
        for (TgFile imageFile : queueItem.getFiles()) {
            String path = fileService.getTempFile(tempDir, queueItem.getUserId(), TAG, imageFile.getFileId(), "File-" + i + "." + imageFile.getFormat().getExt());
            imageFile.setFilePath(path);
            Progress downloadProgress = progress(queueItem, i, queueItem.getFiles().size(), locale);
            imageFile.setProgress(downloadProgress);
            imageFile.setDeleteParentDir(true);
            ++i;
        }

        return tgFiles;
    }

    private Progress progress(ConversionQueueItem queueItem, int current, int total, Locale locale) {
        Progress progress = new Progress();
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(messageBuilder.getFilesDownloadingProgressMessage(queueItem, current, total, locale));
        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        if (current + 1 < total) {
            String completionMessage = messageBuilder.getFilesDownloadingProgressMessage(queueItem, current + 1, total, locale);
            String calculated = localisationService.getMessage(MESSAGE_CALCULATED, locale);
            completionMessage = String.format(completionMessage, 0, calculated, calculated);
            progress.setAfterProgressCompletionMessage(completionMessage);
            progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));
        } else {
            String completionMessage = messageBuilder.getConversionProcessingMessage(queueItem, Collections.emptySet(),
                    ConversionStep.CONVERTING, locale);
            progress.setAfterProgressCompletionMessage(completionMessage);
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));
        }

        return progress;
    }
}

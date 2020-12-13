package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.job.DownloadExtra;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.image.device.Image2PdfDevice;
import ru.gadjini.telegram.converter.service.image.device.ImageMagickDevice;
import ru.gadjini.telegram.converter.service.progress.ProgressBuilder;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Images2PdfTiffConverter extends BaseAny2AnyConverter {

    private static final String TAG = "images2pdftiff";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGES), List.of(Format.PDF, Format.TIFF)
    );

    private ImageMagickDevice magickDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private Image2PdfDevice image2PdfDevice;

    private ProgressBuilder progressBuilder;

    @Autowired
    public Images2PdfTiffConverter(ImageMagickDevice magickDevice,
                                   LocalisationService localisationService, UserService userService,
                                   Image2PdfDevice image2PdfDevice, ProgressBuilder progressBuilder) {
        super(MAP);
        this.magickDevice = magickDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.image2PdfDevice = image2PdfDevice;
        this.progressBuilder = progressBuilder;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        Collection<TgFile> tgFiles = prepareFilesToDownload(conversionQueueItem);

        DownloadExtra extra = new DownloadExtra(conversionQueueItem.getFiles(), 0);
        fileDownloadService().createDownload(tgFiles.iterator().next(), conversionQueueItem.getId(), conversionQueueItem.getUserId(), extra);

        return tgFiles.size();
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

            SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), TAG, targetFormat.getExt());
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
        String tempDir = getFileService().getTempDir(queueItem.getUserId(), TAG);

        int i = 0;
        for (TgFile imageFile : queueItem.getFiles()) {
            String path = getFileService().getTempFile(tempDir, queueItem.getUserId(), TAG, imageFile.getFileId(), "File-" + i + "." + imageFile.getFormat().getExt());
            imageFile.setFilePath(path);
            Progress downloadProgress = progressBuilder.buildFilesDownloadProgress(queueItem, i, queueItem.getFiles().size());
            imageFile.setProgress(downloadProgress);
            imageFile.setDeleteParentDir(true);
            ++i;
        }

        return tgFiles;
    }
}

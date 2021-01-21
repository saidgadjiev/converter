package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.job.DownloadExtra;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.progress.ProgressBuilder;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class BaseAny2AnyConverter implements Any2AnyConverter {

    private final Map<List<Format>, List<Format>> map;

    private TempFileService fileService;

    private FileDownloadService fileDownloadService;

    private ProgressBuilder progressBuilder;

    protected BaseAny2AnyConverter(Map<List<Format>, List<Format>> map) {
        this.map = map;
    }

    @Autowired
    public void setFileDownloadService(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }

    @Autowired
    public void setProgressBuilder(ProgressBuilder progressBuilder) {
        this.progressBuilder = progressBuilder;
    }

    @Autowired
    public void setFileService(TempFileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public final boolean accept(Format format, Format targetFormat) {
        return isConvertAvailable(format, targetFormat);
    }

    @Override
    public Map<List<Format>, List<Format>> getConversionMap() {
        return map;
    }

    TempFileService getFileService() {
        return fileService;
    }

    FileDownloadService fileDownloadService() {
        return fileDownloadService;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return createDownloads0(conversionQueueItem);
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem, Supplier<Boolean> cancelChecker, Supplier<Boolean> canceledByUserChecker) {
        try {
            return doConvert(fileQueueItem);
        } finally {
            if (cancelChecker.get()) {
                if (canceledByUserChecker.get()) {
                    doDeleteFiles(fileQueueItem);
                }
            } else {
                doDeleteFiles(fileQueueItem);
            }
        }
    }

    int createDownloadsWithThumb(ConversionQueueItem conversionQueueItem) {
        int total = createDownloads0(conversionQueueItem);
        if (StringUtils.isNotBlank(conversionQueueItem.getFirstFile().getThumb())) {
            TgFile thumb = new TgFile();
            thumb.setFileId(conversionQueueItem.getFirstFile().getThumb());
            thumb.setFormat(Format.JPG);
            fileDownloadService().createDownload(thumb, conversionQueueItem.getId(), conversionQueueItem.getUserId());
            ++total;
        }

        return total;
    }

    private void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFiles().forEach(SmartTempFile::smartDelete);
    }

    private int createDownloads0(ConversionQueueItem conversionQueueItem) {
        if (conversionQueueItem.getFiles().size() > 1) {
            int i = 0;
            for (TgFile imageFile : conversionQueueItem.getFiles()) {
                Progress downloadProgress = progressBuilder.buildFilesDownloadProgress(conversionQueueItem, i, conversionQueueItem.getFiles().size());
                imageFile.setProgress(downloadProgress);
                ++i;
            }
            DownloadExtra extra = new DownloadExtra(conversionQueueItem.getFiles(), 0);
            fileDownloadService.createDownload(conversionQueueItem.getFirstFile(), conversionQueueItem.getId(), conversionQueueItem.getUserId(), extra);
        } else {
            conversionQueueItem.getFirstFile().setProgress(progress(conversionQueueItem));
            fileDownloadService.createDownload(conversionQueueItem.getFirstFile(), conversionQueueItem.getId(), conversionQueueItem.getUserId(), null);
        }

        return conversionQueueItem.getFiles().size();
    }

    final SmartTempFile downloadThumb(ConversionQueueItem fileQueueItem) {
        if (StringUtils.isNotBlank(fileQueueItem.getFirstFile().getThumb())) {
            return fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFile().getThumb());
        } else {
            return null;
        }
    }

    private Progress progress(ConversionQueueItem queueItem) {
        return progressBuilder.buildFileDownloadProgress(queueItem);
    }

    private boolean isConvertAvailable(Format src, Format target) {
        return getTargetFormats(src).contains(target);
    }

    private List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<List<Format>, List<Format>> entry : map.entrySet()) {
            if (entry.getKey().contains(srcFormat)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }

    protected abstract ConvertResult doConvert(ConversionQueueItem conversionQueueItem);
}

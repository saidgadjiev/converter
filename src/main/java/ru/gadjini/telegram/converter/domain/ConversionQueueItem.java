package ru.gadjini.telegram.converter.domain;

import org.apache.commons.lang3.StringUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.DownloadQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.domain.WorkQueueItem;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConversionQueueItem extends WorkQueueItem {

    public static final String TYPE = "conversion_queue";

    public static final String EXCEPTION = "exception";

    public static final String TARGET_FORMAT = "target_format";

    public static final String FILES = "files";

    public static final String RESULT_FILE_ID = "result_file_id";

    public static final String DOWNLOADS = "downloads";

    public static final String EXTRA = "extra";

    public static final String FILES_JSON = "files_json";

    public static final String TOTAL_FILES_TO_DOWNLOAD = "total_files_to_download";

    private List<TgFile> files = new ArrayList<>();

    private Format targetFormat;

    private String resultFileId;

    private List<DownloadQueueItem> downloadQueueItems;

    private Object extra;

    private long totalFilesToDownload;

    public Format getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(Format targetFormat) {
        this.targetFormat = targetFormat;
    }

    public List<TgFile> getFiles() {
        return files;
    }

    public void addFile(TgFile file) {
        this.files.add(file);
    }

    public void setFiles(List<TgFile> files) {
        this.files = files;
    }

    public TgFile getFirstFile() {
        return files.iterator().next();
    }

    public String getResultFileId() {
        return resultFileId;
    }

    public void setResultFileId(String resultFileId) {
        this.resultFileId = resultFileId;
    }

    public String getFirstFileId() {
        return getFirstFile().getFileId();
    }

    public Format getFirstFileFormat() {
        return getFirstFile().getFormat();
    }

    public String getFirstFileName() {
        return getFirstFile().getFileName();
    }

    public List<DownloadQueueItem> getDownloadQueueItems() {
        return downloadQueueItems;
    }

    public void setDownloadQueueItems(List<DownloadQueueItem> downloadQueueItems) {
        this.downloadQueueItems = downloadQueueItems;
    }

    public List<SmartTempFile> getDownloadedFiles() {
        return files == null ? Collections.emptyList() :
                files.stream()
                        .map(file -> getDownloadedFileOrThrow(file.getFileId()))
                        .collect(Collectors.toList());
    }

    public List<SmartTempFile> getDownloadedFilesWithoutThumb() {
        return files == null ? Collections.emptyList() :
                files.stream()
                        .filter(tgFile -> !Objects.equals(tgFile.getFileId(), getFirstFile().getThumb()))
                        .map(file -> getDownloadedFileOrNull(file.getFileId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    public SmartTempFile getDownloadedFileOrThrow(String fileId) {
        SmartTempFile downloadedFile = getDownloadedFileOrNull(fileId);
        if (downloadedFile == null) {
            throw new IllegalArgumentException("Downloaded file not found for " + fileId);
        }

        return downloadedFile;
    }

    public SmartTempFile getDownloadedFileOrNull(String fileId) {
        DownloadQueueItem queueItem = downloadQueueItems.stream().filter(
                downloadingQueueItem -> downloadingQueueItem.getFile().getFileId().equals(fileId)
        ).findAny().orElse(null);

        if (queueItem == null) {
            return null;
        }
        if (StringUtils.isBlank(queueItem.getFilePath())) {
            return null;
        }

        return new SmartTempFile(new File(queueItem.getFilePath()), queueItem.isDeleteParentDir());
    }

    public void setExtra(Object extra) {
        this.extra = extra;
    }

    public Object getExtra() {
        return extra;
    }

    public long getTotalFilesToDownload() {
        return totalFilesToDownload;
    }

    public void setTotalFilesToDownload(long totalFilesToDownload) {
        this.totalFilesToDownload = totalFilesToDownload;
    }

    @Override
    public long getSize() {
        return files.stream().mapToLong(TgFile::getSize).sum();
    }
}

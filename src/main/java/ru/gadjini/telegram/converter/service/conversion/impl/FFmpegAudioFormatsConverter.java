package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.FileSource;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegAudioFormatsConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegAudioFormatsConverter.class);

    private static final String TAG = "ffmpegaudio";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(AAC), List.of(AMR, AIFF, FLAC, MP3, OGG, WAV, WMA));
        put(List.of(AMR), List.of(AAC, AIFF, FLAC, MP3, OGG, WAV, WMA));
        put(List.of(AIFF), List.of(AMR, AAC, FLAC, MP3, OGG, WAV, WMA));
        put(List.of(FLAC), List.of(AMR, AAC, AIFF, MP3, OGG, WAV, WMA));
        put(List.of(MP3), List.of(AMR, AAC, AIFF, FLAC, OGG, WAV, WMA));
        put(List.of(OGG), List.of(AMR, AAC, AIFF, FLAC, MP3, WAV, WMA));
        put(List.of(WAV), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WMA));
        put(List.of(WMA), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WAV));
        put(List.of(M4A), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA));
        put(List.of(M4B), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA));
    }};

    private FFmpegDevice fFmpegDevice;

    private TempFileService fileService;

    private FileManager fileManager;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegAudioFormatsConverter(FFmpegDevice fFmpegDevice, TempFileService fileService, FileManager fileManager, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);

            SmartTempFile out = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), getOptions(fileQueueItem.getTargetFormat()));

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

                if (FileSource.AUDIO.equals(fileQueueItem.getFirstFile().getSource())
                        && fileQueueItem.getTargetFormat().canBeSentAsAudio()) {
                    SmartTempFile thumbFile = null;
                    if (StringUtils.isNotBlank(fileQueueItem.getFirstFile().getThumb())) {
                        thumbFile = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getThumb(), TAG, Format.JPG.getExt());
                        fileManager.downloadFileByFileId(fileQueueItem.getFirstFile().getThumb(), 1, thumbFile);
                    }

                    if (fileQueueItem.getFirstFile().getDuration() == null) {
                        try {
                            long durationInSeconds = fFprobeDevice.getDurationInSeconds(out.getAbsolutePath());
                            fileQueueItem.getFirstFile().setDuration((int) durationInSeconds);
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                            fileQueueItem.getFirstFile().setDuration(0);
                        }
                    }

                    return new AudioResult(fileName, out, fileQueueItem.getFirstFile().getAudioPerformer(),
                            fileQueueItem.getFirstFile().getAudioTitle(), thumbFile, fileQueueItem.getFirstFile().getDuration());
                }

                return new FileResult(fileName, out);
            } catch (Throwable e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            file.smartDelete();
        }
    }

    private String[] getOptions(Format target) {
        if (target == AMR) {
            return new String[]{
                    "-ar", "8000", "-ac", "1"
            };
        }
        if (target == OGG) {
            return new String[]{
                    "-c:a", "libvorbis", "-q:a", "4"
            };
        }
        return new String[0];
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.timidity.TimidityDevice;
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

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MID;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WAV;

@Component
@SuppressWarnings("CPD-START")
public class Timidity2WavFormatConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Timidity2WavFormatConverter.class);

    private static final String TAG = "timiditywav";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(MID), List.of(WAV));
    }};

    private TimidityDevice timidityDevice;

    private TempFileService fileService;

    private FileManager fileManager;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public Timidity2WavFormatConverter(TimidityDevice timidityDevice, TempFileService fileService,
                                       FileManager fileManager, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.timidityDevice = timidityDevice;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, MID.getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);

            SmartTempFile out = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, WAV.getExt());
            try {
                timidityDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), "-Ow", "-o");

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), WAV.getExt());

                SmartTempFile thumbFile = downloadThumb(fileQueueItem);
                if (FileSource.AUDIO.equals(fileQueueItem.getFirstFile().getSource())
                        && fileQueueItem.getTargetFormat().canBeSentAsAudio()) {
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

                return new FileResult(fileName, out, thumbFile);
            } catch (Throwable e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            file.smartDelete();
        }
    }
}

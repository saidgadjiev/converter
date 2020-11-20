package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.FileSource;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

public abstract class BaseAudioConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAudioConverter.class);

    private static final String TAG = "baseaudio";

    private FFprobeDevice fFprobeDevice;

    protected BaseAudioConverter(Map<List<Format>, List<Format>> map) {
        super(map);
    }

    @Autowired
    public void setfFprobeDevice(FFprobeDevice fFprobeDevice) {
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            getFileManager().downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);

            SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                doConvert(file, out, fileQueueItem);
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

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

    protected abstract void doConvert(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem);
}

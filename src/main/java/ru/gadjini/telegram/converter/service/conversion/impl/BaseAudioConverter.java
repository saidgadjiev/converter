package ru.gadjini.telegram.converter.service.conversion.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VoiceResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.FileSource;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

public abstract class BaseAudioConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAudioConverter.class);

    private static final String TAG = "baseaudio";

    private FFprobeDevice fFprobeDevice;

    private ConversionMessageBuilder messageBuilder;

    private UserService userService;

    private Gson gson;

    protected BaseAudioConverter(Map<List<Format>, List<Format>> map) {
        super(map);
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setMessageBuilder(ConversionMessageBuilder messageBuilder) {
        this.messageBuilder = messageBuilder;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Autowired
    public void setfFprobeDevice(FFprobeDevice fFprobeDevice) {
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());
        Format targetFormat = getTargetFormat(fileQueueItem);

        SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG,
                targetFormat.getExt());
        try {
            doConvertAudio(file, out, fileQueueItem);
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), targetFormat.getExt());

            SmartTempFile thumbFile = downloadThumb(fileQueueItem);
            if (FileSource.AUDIO.equals(fileQueueItem.getFirstFile().getSource()) && targetFormat.canBeSentAsAudio()
                    || fileQueueItem.getTargetFormat().equals(Format.VOICE)) {
                if (fileQueueItem.getFirstFile().getDuration() == null) {
                    try {
                        long durationInSeconds = fFprobeDevice.getDurationInSeconds(out.getAbsolutePath());
                        fileQueueItem.getFirstFile().setDuration((int) durationInSeconds);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        fileQueueItem.getFirstFile().setDuration(0);
                    }
                }

                String caption = null;
                if (fileQueueItem.getTargetFormat() == Format.COMPRESS) {
                    caption = messageBuilder.getCompressionInfoMessage(fileQueueItem.getSize(), out.length(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));
                }
                if (fileQueueItem.getTargetFormat() == Format.VOICE) {
                    return new VoiceResult(fileName, out, fileQueueItem.getFirstFile().getDuration(), caption);
                } else {
                    return new AudioResult(fileName, out, fileQueueItem.getFirstFile().getAudioPerformer(),
                            fileQueueItem.getFirstFile().getAudioTitle(), thumbFile, fileQueueItem.getFirstFile().getDuration(), caption);
                }
            }

            return new FileResult(fileName, out, thumbFile);
        } catch (Throwable e) {
            out.smartDelete();
            throw e;
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    private Format getTargetFormat(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.COMPRESS && fileQueueItem.getExtra() != null) {
            SettingsState settingsState = gson.fromJson((JsonElement) fileQueueItem.getExtra(), SettingsState.class);
            if (settingsState.getTargetFormat() != null) {
                return settingsState.getTargetFormat();
            }
        }

        return fileQueueItem.getTargetFormat() == Format.COMPRESS ? fileQueueItem.getFirstFileFormat() : fileQueueItem.getTargetFormat();
    }

    protected abstract void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem);
}

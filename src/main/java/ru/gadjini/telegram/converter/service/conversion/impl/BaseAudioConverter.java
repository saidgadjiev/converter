package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VoiceResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.converter.service.conversion.impl.FFmpegAudioCompressConverter.DEFAULT_AUDIO_COMPRESS_FORMAT;

public abstract class BaseAudioConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAudioConverter.class);

    private static final String TAG = "baseaudio";

    private FFprobeDevice fFprobeDevice;

    private ConversionMessageBuilder messageBuilder;

    private UserService userService;

    private Jackson jackson;

    protected BaseAudioConverter(Map<List<Format>, List<Format>> map) {
        super(map);
    }

    @Autowired
    public void setJackson(Jackson jackson) {
        this.jackson = jackson;
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
    public void setfFprobeDevice(FFprobeDevice fFprobeDevice) {
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        Format targetFormat = getTargetFormat(fileQueueItem);

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG,
                targetFormat.getExt());
        try {
            doConvertAudio(file, result, fileQueueItem);
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), targetFormat.getExt());

            SmartTempFile thumbFile = downloadThumb(fileQueueItem);
            if (targetFormat.canBeSentAsAudio() || fileQueueItem.getTargetFormat().equals(Format.VOICE)) {
                if (fileQueueItem.getFirstFile().getDuration() == null) {
                    try {
                        long durationInSeconds = fFprobeDevice.getDurationInSeconds(result.getAbsolutePath());
                        fileQueueItem.getFirstFile().setDuration((int) durationInSeconds);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        fileQueueItem.getFirstFile().setDuration(0);
                    }
                }

                String caption = null;
                if (fileQueueItem.getTargetFormat() == Format.COMPRESS) {
                    caption = messageBuilder.getCompressionInfoMessage(fileQueueItem.getSize(), result.length(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));
                }
                if (fileQueueItem.getTargetFormat() == Format.VOICE) {
                    return new VoiceResult(fileName, result, fileQueueItem.getFirstFile().getDuration(), caption);
                } else {
                    return new AudioResult(fileName, result, fileQueueItem.getFirstFile().getAudioPerformer(),
                            fileQueueItem.getFirstFile().getAudioTitle(), thumbFile, fileQueueItem.getFirstFile().getDuration(), caption);
                }
            }

            return new FileResult(fileName, result, thumbFile);
        } catch (UserException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }


    private Format getTargetFormat(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.COMPRESS && fileQueueItem.getExtra() != null) {
            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            return settingsState.getFormatOrDefault(DEFAULT_AUDIO_COMPRESS_FORMAT);
        }

        return fileQueueItem.getTargetFormat() == Format.COMPRESS ? DEFAULT_AUDIO_COMPRESS_FORMAT : fileQueueItem.getTargetFormat();
    }

    protected abstract void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem conversionQueueItem) throws InterruptedException;
}

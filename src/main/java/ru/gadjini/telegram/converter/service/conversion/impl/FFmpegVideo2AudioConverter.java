package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.*;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
public class FFmpegVideo2AudioConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegVideo2AudioConverter.class);

    private static final String TAG = "ffmpegvideo";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(filter(FormatCategory.VIDEO), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A, VOICE, RA));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioConversionHelper audioConversionHelper;

    private FFmpegHelper fFmpegHelper;

    @Autowired
    public FFmpegVideo2AudioConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                      FFmpegAudioConversionHelper audioConversionHelper,
                                      FFmpegHelper fFmpegHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.audioConversionHelper = audioConversionHelper;
        this.fFmpegHelper = fFmpegHelper;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            fFmpegHelper.validateVideoIntegrity(file);
            List<FFprobeDevice.Stream> audioStreams = fFprobeDevice.getAudioStreams(file.getAbsolutePath());
            ConvertResults convertResults = new ConvertResults();
            for (int streamIndex = 0; streamIndex < audioStreams.size(); streamIndex++) {
                FFprobeDevice.Stream audioStream = audioStreams.get(streamIndex);
                SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

                try {
                    FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().mapAudio(streamIndex);
                    audioConversionHelper.addAudioOptions(fileQueueItem.getTargetFormat(), commandBuilder);
                    fFmpegDevice.convert(file.getAbsolutePath(), result.getAbsolutePath(), commandBuilder.build());

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(),
                            String.valueOf(streamIndex), fileQueueItem.getTargetFormat().getExt());

                    if (fileQueueItem.getTargetFormat().canBeSentAsAudio()
                            || fileQueueItem.getTargetFormat() == VOICE) {
                        long durationInSeconds = 0;
                        try {
                            durationInSeconds = fFprobeDevice.getDurationInSeconds(result.getAbsolutePath());
                        } catch (InterruptedException e) {
                            throw e;
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }

                        if (fileQueueItem.getTargetFormat().equals(VOICE)) {
                            convertResults.addResult(new VoiceResult(fileName, result, (int) durationInSeconds, audioStream.getLanguage()));
                        } else {
                            convertResults.addResult(new AudioResult(fileName, result, (int) durationInSeconds, audioStream.getLanguage()));
                        }
                    } else {
                        convertResults.addResult(new FileResult(fileName, result, audioStream.getLanguage()));
                    }
                } catch (Exception e) {
                    tempFileService().delete(result);
                    throw e;
                }
            }

            return convertResults;
        } catch (CorruptedVideoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConvertException(e);
        }
    }
}

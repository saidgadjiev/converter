package ru.gadjini.telegram.converter.service.conversion.impl.extraction.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VoiceResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.extraction.BaseFromVideoByLanguageExtractor;
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
public class FFmpegAudioFromVideoExtractor extends BaseFromVideoByLanguageExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegAudioFromVideoExtractor.class);

    private static final String TAG = "ffmpegaudio";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(filter(FormatCategory.VIDEO), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A, VOICE, RA));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioConversionHelper audioConversionHelper;

    @Autowired
    public FFmpegAudioFromVideoExtractor(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                         FFmpegAudioConversionHelper audioConversionHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.audioConversionHelper = audioConversionHelper;
    }

    @Override
    protected String getChooseLanguageMessageCode() {
        return ConverterMessagesProperties.MESSAGE_CHOOSE_AUDIO_LANGUAGE;
    }

    @Override
    public ConversionResult doExtract(ConversionQueueItem fileQueueItem, SmartTempFile file,
                                            List<FFprobeDevice.Stream> audioStreams, int streamIndex) throws InterruptedException {
        FFprobeDevice.Stream audioStream = audioStreams.get(streamIndex);
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite()
                    .input(file.getAbsolutePath()).mapAudio(streamIndex);

            if (fileQueueItem.getTargetFormat().canBeSentAsVoice()) {
                audioConversionHelper.convertAudioCodecsForTelegramVoice(commandBuilder, fileQueueItem.getTargetFormat());
            } else {
                audioConversionHelper.convertAudioCodecs(commandBuilder, fileQueueItem.getTargetFormat());
            }
            audioConversionHelper.addAudioTargetOptions(commandBuilder, fileQueueItem.getTargetFormat());
            commandBuilder.out(result.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.buildFullCommand());

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
                    return new VoiceResult(fileName, result, (int) durationInSeconds, audioStream.getLanguage());
                } else {
                    return new AudioResult(fileName, result, (int) durationInSeconds, audioStream.getLanguage());
                }
            } else {
                return new FileResult(fileName, result, audioStream.getLanguage());
            }
        } catch (Exception e) {
            tempFileService().delete(result);
            throw e;
        }
    }

    @Override
    protected String getStreamSpecifier() {
        return FFprobeDevice.Stream.AUDIO_CODEC_TYPE;
    }

    @Override
    protected String getStreamsToExtractNotFoundMessage() {
        return ConverterMessagesProperties.MESSAGE_AUDIO_STREAMS_NOT_FOUND;
    }
}

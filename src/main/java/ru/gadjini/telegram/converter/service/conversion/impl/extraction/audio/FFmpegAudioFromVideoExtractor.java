package ru.gadjini.telegram.converter.service.conversion.impl.extraction.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VoiceResult;
import ru.gadjini.telegram.converter.service.conversion.impl.extraction.BaseFromVideoByLanguageExtractor;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private final FFmpegConversionContextPreparerChain conversionContextPreparerChain;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    @Autowired
    public FFmpegAudioFromVideoExtractor(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                         UserService userService,
                                         FFmpegCommandBuilderFactory commandBuilderFactory,
                                         FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;

        this.commandBuilderChain = commandBuilderFactory.quiteInput();
        commandBuilderChain.setNext(commandBuilderFactory.audioConversion())
                .setNext(commandBuilderFactory.audioChannelMapFilter())
                .setNext(commandBuilderFactory.audioConversionDefaultOptions())
                .setNext(commandBuilderFactory.output());

        this.conversionContextPreparerChain = contextPreparerChainFactory.extractAudioPreparer();
        conversionContextPreparerChain.setNext(contextPreparerChainFactory.telegramVoiceContextPreparer());
    }

    @Override
    protected String getChooseLanguageMessageCode() {
        return ConverterMessagesProperties.MESSAGE_CHOOSE_AUDIO_LANGUAGE;
    }

    @Override
    public ConversionResult doExtract(ConversionQueueItem fileQueueItem, SmartTempFile file,
                                      List<FFprobeDevice.FFProbeStream> audioStreams, int streamIndex) throws InterruptedException {
        FFprobeDevice.FFProbeStream audioStream = audioStreams.get(streamIndex);
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            FFmpegConversionContext conversionContext = FFmpegConversionContext.from(file, result,
                    fileQueueItem.getTargetFormat(), List.of(audioStream))
                    .putExtra(FFmpegConversionContext.EXTRACT_AUDIO_INDEX, streamIndex);
            conversionContextPreparerChain.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);
            fFmpegDevice.execute(command.toCmd());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(),
                    String.valueOf(streamIndex), fileQueueItem.getTargetFormat().getExt());

            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            if (fileQueueItem.getTargetFormat().canBeSentAsAudio()) {
                long durationInSeconds = 0;
                try {
                    durationInSeconds = fFprobeDevice.getDurationInSeconds(result.getAbsolutePath());
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }

                if (fileQueueItem.getTargetFormat().equals(VOICE)) {
                    return new VoiceResult(fileName, result, (int) durationInSeconds, getLanguageMessage(audioStream.getLanguage(), locale));
                } else {
                    return new AudioResult(fileName, result, (int) durationInSeconds, getLanguageMessage(audioStream.getLanguage(), locale));
                }
            } else {
                return new FileResult(fileName, result, getLanguageMessage(audioStream.getLanguage(), locale));
            }
        } catch (Exception e) {
            tempFileService().delete(result);
            throw e;
        }
    }

    @Override
    protected String getStreamSpecifier() {
        return FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE;
    }

    @Override
    protected String getStreamsToExtractNotFoundMessage() {
        return ConverterMessagesProperties.MESSAGE_AUDIO_STREAMS_NOT_FOUND;
    }
}

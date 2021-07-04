package ru.gadjini.telegram.converter.service.conversion.impl.audio.extraction;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.*;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
public class FFmpegAudioFromVideoExtractor extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegAudioFromVideoExtractor.class);

    private static final String TAG = "ffmpegaudio";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(filter(FormatCategory.VIDEO), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A, VOICE, RA));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioConversionHelper audioConversionHelper;

    private FFmpegVideoHelper fFmpegVideoHelper;

    private UserService userService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private Jackson jackson;

    @Autowired
    public FFmpegAudioFromVideoExtractor(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                         FFmpegAudioConversionHelper audioConversionHelper,
                                         FFmpegVideoHelper fFmpegVideoHelper, UserService userService,
                                         LocalisationService localisationService,
                                         InlineKeyboardService inlineKeyboardService,
                                         CommandStateService commandStateService, Jackson jackson) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.audioConversionHelper = audioConversionHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.userService = userService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.jackson = jackson;
    }

    @Override
    public boolean needToSendProgressMessage(ConversionQueueItem conversionQueueItem, AtomicInteger progressMessageId) {
        if (conversionQueueItem.getExtra() != null) {
            AudioExtractionState audioExtractionState = commandStateService.getState(conversionQueueItem.getUserId(),
                    ConverterCommandNames.EXTRACT_AUDIO, true, AudioExtractionState.class);
            progressMessageId.set(audioExtractionState.getProgressMessageId());

            return false;
        }

        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        if (conversionQueueItem.getExtra() != null) {
            AudioExtractionState audioExtractionState = commandStateService.getState(conversionQueueItem.getUserId(),
                    ConverterCommandNames.EXTRACT_AUDIO, true, AudioExtractionState.class);

            conversionQueueItem.getFirstFile().setFilePath(audioExtractionState.getFilePath());
            getFileDownloadService().createCompletedDownloads(
                    conversionQueueItem.getFiles(), conversionQueueItem.getId(), conversionQueueItem.getUserId(), null
            );

            return conversionQueueItem.getFiles().size();
        } else {
            return super.createDownloads(conversionQueueItem);
        }
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);

            List<FFprobeDevice.Stream> audioStreams = fFprobeDevice.getAudioStreams(file.getAbsolutePath());
            if (audioStreams.isEmpty()) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_AUDIO_STREAMS_NOT_FOUND,
                        userService.getLocaleOrDefault(fileQueueItem.getUserId()))).setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }

            if (fileQueueItem.getExtra() != null) {
                SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
                ConvertResults convertResults = new ConvertResults();
                for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); audioStreamIndex++) {
                    FFprobeDevice.Stream audioStream = audioStreams.get(audioStreamIndex);
                    if (settingsState.getLanguageToExtract().equals(audioStream.getLanguage())) {
                        convertResults.addResult(doExtractAudio(fileQueueItem, file, audioStreams, audioStreamIndex));
                    }
                }

                return convertResults;
            }

            if (audioStreams.size() == 1) {
                return doExtractAudio(fileQueueItem, file, audioStreams, 0);
            } else if (audioStreams.stream().allMatch(a -> StringUtils.isNotBlank(a.getLanguage()))
                    && audioStreams.stream().map(FFprobeDevice.Stream::getLanguage).distinct().count() > 1) {
                List<String> languages = audioStreams.stream().map(FFprobeDevice.Stream::getLanguage).distinct().collect(Collectors.toList());
                commandStateService.setState(fileQueueItem.getUserId(), ConverterCommandNames.EXTRACT_AUDIO, createState(fileQueueItem));

                return new MessageResult(
                        SendMessage.builder()
                                .chatId(String.valueOf(fileQueueItem.getUserId()))
                                .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_AUDIO_LANGUAGE,
                                        userService.getLocaleOrDefault(fileQueueItem.getUserId())))
                                .replyMarkup(inlineKeyboardService.getAudioLanguagesKeyboard(languages))
                                .parseMode(ParseMode.HTML)
                                .build(),
                        false
                );
            } else {
                ConvertResults convertResults = new ConvertResults();
                for (int streamIndex = 0; streamIndex < audioStreams.size(); streamIndex++) {
                    convertResults.addResult(doExtractAudio(fileQueueItem, file, audioStreams, streamIndex));
                }

                return convertResults;
            }
        } catch (UserException | CorruptedVideoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConvertException(e);
        }
    }

    private ConversionResult doExtractAudio(ConversionQueueItem fileQueueItem, SmartTempFile file,
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

    private AudioExtractionState createState(ConversionQueueItem queueItem) {
        AudioExtractionState audioExtractionState = new AudioExtractionState();
        audioExtractionState.setFile(queueItem.getFirstFile());
        SmartTempFile file = queueItem.getDownloadedFileOrThrow(queueItem.getFirstFileId());
        audioExtractionState.setFilePath(file.getAbsolutePath());
        audioExtractionState.setReplyToMessageId(queueItem.getReplyToMessageId());
        audioExtractionState.setTargetFormat(queueItem.getTargetFormat());
        audioExtractionState.setUserId(queueItem.getUserId());
        audioExtractionState.setProgressMessageId(queueItem.getProgressMessageId());

        return audioExtractionState;
    }
}

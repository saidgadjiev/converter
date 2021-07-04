package ru.gadjini.telegram.converter.service.conversion.impl.extraction;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResults;
import ru.gadjini.telegram.converter.service.conversion.api.result.MessageResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class BaseFromVideoByLanguageExtractor extends BaseAny2AnyConverter {

    private CommandStateService commandStateService;

    private FFmpegVideoHelper fFmpegVideoHelper;

    private UserService userService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private FFprobeDevice fFprobeDevice;

    private Jackson jackson;

    protected BaseFromVideoByLanguageExtractor(Map<List<Format>, List<Format>> map) {
        super(map);
    }

    @Autowired
    public void setfFprobeDevice(FFprobeDevice fFprobeDevice) {
        this.fFprobeDevice = fFprobeDevice;
    }

    @Autowired
    public void setCommandStateService(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setInlineKeyboardService(InlineKeyboardService inlineKeyboardService) {
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Autowired
    public void setfFmpegVideoHelper(FFmpegVideoHelper fFmpegVideoHelper) {
        this.fFmpegVideoHelper = fFmpegVideoHelper;
    }

    @Autowired
    public void setJackson(Jackson jackson) {
        this.jackson = jackson;
    }

    @Autowired
    public void setLocalisationService(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    public boolean needToSendProgressMessage(ConversionQueueItem conversionQueueItem, AtomicInteger progressMessageId) {
        if (conversionQueueItem.getExtra() != null) {
            ExtractionByLanguageState audioExtractionState = commandStateService.getState(conversionQueueItem.getUserId(),
                    ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE, true, ExtractionByLanguageState.class);
            progressMessageId.set(audioExtractionState.getProgressMessageId());

            return false;
        }

        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        if (conversionQueueItem.getExtra() != null) {
            ExtractionByLanguageState audioExtractionState = commandStateService.getState(conversionQueueItem.getUserId(),
                    ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE, true, ExtractionByLanguageState.class);

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
    public final ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);

            String streamSpecifier = getStreamSpecifier();
            List<FFprobeDevice.Stream> streamsToExtract;
            if (FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(streamSpecifier)) {
                streamsToExtract = fFprobeDevice.getAudioStreams(file.getAbsolutePath());
            } else {
                streamsToExtract = fFprobeDevice.getSubtitleStreams(file.getAbsolutePath());
            }
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            if (streamsToExtract.isEmpty()) {
                throw new UserException(localisationService.getMessage(getStreamsToExtractNotFoundMessage(),
                        locale)).setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }

            if (fileQueueItem.getExtra() != null) {
                SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
                ConvertResults convertResults = new ConvertResults();
                for (int audioStreamIndex = 0; audioStreamIndex < streamsToExtract.size(); audioStreamIndex++) {
                    FFprobeDevice.Stream audioStream = streamsToExtract.get(audioStreamIndex);
                    if (StringUtils.isBlank(settingsState.getLanguageToExtract())
                            || settingsState.getLanguageToExtract().equals(audioStream.getLanguage())) {
                        convertResults.addResult(doExtract(fileQueueItem, file, streamsToExtract, audioStreamIndex));
                    }
                }

                return convertResults;
            }

            if (streamsToExtract.size() == 1) {
                return doExtract(fileQueueItem, file, streamsToExtract, 0);
            } else if (streamsToExtract.stream().anyMatch(a -> StringUtils.isNotBlank(a.getLanguage()))) {
                List<String> languages = streamsToExtract.stream().map(FFprobeDevice.Stream::getLanguage).distinct().collect(Collectors.toList());
                commandStateService.setState(fileQueueItem.getUserId(), ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE, createState(fileQueueItem, languages));

                return new MessageResult(
                        SendMessage.builder()
                                .chatId(String.valueOf(fileQueueItem.getUserId()))
                                .text(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_CHOOSE_AUDIO_LANGUAGE,
                                        locale))
                                .replyMarkup(inlineKeyboardService.getLanguagesRootKeyboard(locale))
                                .parseMode(ParseMode.HTML)
                                .build(),
                        false
                );
            } else {
                ConvertResults convertResults = new ConvertResults();
                for (int streamIndex = 0; streamIndex < streamsToExtract.size(); streamIndex++) {
                    convertResults.addResult(doExtract(fileQueueItem, file, streamsToExtract, streamIndex));
                }

                return convertResults;
            }
        } catch (UserException | CorruptedVideoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConvertException(e);
        }
    }

    protected abstract ConversionResult doExtract(ConversionQueueItem conversionQueueItem, SmartTempFile file,
                                                  List<FFprobeDevice.Stream> streams, int streamIndex) throws InterruptedException;

    protected abstract String getStreamSpecifier();

    protected abstract String getStreamsToExtractNotFoundMessage();

    private ExtractionByLanguageState createState(ConversionQueueItem queueItem, List<String> languages) {
        ExtractionByLanguageState audioExtractionState = new ExtractionByLanguageState();
        audioExtractionState.setFile(queueItem.getFirstFile());
        SmartTempFile file = queueItem.getDownloadedFileOrThrow(queueItem.getFirstFileId());
        audioExtractionState.setFilePath(file.getAbsolutePath());
        audioExtractionState.setReplyToMessageId(queueItem.getReplyToMessageId());
        audioExtractionState.setTargetFormat(queueItem.getTargetFormat());
        audioExtractionState.setUserId(queueItem.getUserId());
        audioExtractionState.setProgressMessageId(queueItem.getProgressMessageId());
        audioExtractionState.setLanguages(languages);

        return audioExtractionState;
    }
}

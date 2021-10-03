package ru.gadjini.telegram.converter.service.conversion.impl.extraction.subtitle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.impl.extraction.BaseFromVideoByLanguageExtractor;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.filter;

@Component
public class FFmpegVideoSubtitlesExtractor extends BaseFromVideoByLanguageExtractor {

    private static final String TAG = "vesubs";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(filter(FormatCategory.VIDEO), filter(FormatCategory.SUBTITLES));
    }};

    private FFmpegDevice fFmpegDevice;

    private UserService userService;

    @Autowired
    public FFmpegVideoSubtitlesExtractor(FFmpegDevice fFmpegDevice, UserService userService) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.userService = userService;
    }

    @Override
    protected String getChooseLanguageMessageCode() {
        return ConverterMessagesProperties.MESSAGE_CHOOSE_SUBTITLE_LANGUAGE;
    }

    @Override
    public ConversionResult doExtract(ConversionQueueItem fileQueueItem, SmartTempFile file,
                                      List<FFprobeDevice.FFProbeStream> subtitleStreams,
                                      int streamIndex) throws InterruptedException {
        FFprobeDevice.FFProbeStream subtitleStream = subtitleStreams.get(streamIndex);
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

        try {
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().mapSubtitles(streamIndex);
            fFmpegDevice.convert(file.getAbsolutePath(), result.getAbsolutePath(), commandBuilder.build());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(),
                    String.valueOf(streamIndex), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, result, getLanguageMessage(subtitleStream.getLanguage(), userService.getLocaleOrDefault(fileQueueItem.getUserId())));
        } catch (Exception e) {
            tempFileService().delete(result);
            throw e;
        }
    }

    @Override
    protected String getStreamSpecifier() {
        return FFprobeDevice.FFProbeStream.SUBTITLE_CODEC_TYPE;
    }

    @Override
    protected String getStreamsToExtractNotFoundMessage() {
        return ConverterMessagesProperties.MESSAGE_SUBTITLE_STREAMS_NOT_FOUND;
    }
}

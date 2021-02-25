package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.common.FFmpegHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
public class VideoCompressConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoCompressConverter.class);

    private static final String TAG = "vcompress";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(TS, MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV), List.of(COMPRESS));
    }};

    private static final String SCALE = "scale=-2:ceil(ih/3)*2";

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private ConversionMessageBuilder messageBuilder;

    private FFmpegHelper fFmpegHelper;

    @Autowired
    public VideoCompressConverter(FFmpegDevice fFmpegDevice, LocalisationService localisationService, UserService userService,
                                  FFprobeDevice fFprobeDevice, ConversionMessageBuilder messageBuilder, FFmpegHelper fFmpegHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.messageBuilder = messageBuilder;
        this.fFmpegHelper = fFmpegHelper;
    }

    @Override
    public ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile result = getFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegHelper.removeExtraVideoStreams(allStreams);

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            List<FFprobeDevice.Stream> videoStreams = allStreams.stream().filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())).collect(Collectors.toList());
            for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); videoStreamIndex++) {
                commandBuilder.mapVideo(videoStreamIndex);
                fFmpegHelper.addFastestVideoCodecOptions(commandBuilder, file, result, videoStreams.get(videoStreamIndex), videoStreamIndex, SCALE);
                commandBuilder.filterVideo(videoStreamIndex, SCALE);
            }
            if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
                commandBuilder.mapAudio().copyAudio();
            }
            if (allStreams.stream().anyMatch(s -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))) {
                if (fFmpegHelper.isSubtitlesCopyable(file, result)) {
                    commandBuilder.mapSubtitles().copySubtitles();
                } else if (FFmpegHelper.isSubtitlesSupported(fileQueueItem.getFirstFileFormat())) {
                    commandBuilder.mapSubtitles();
                    FFmpegHelper.addSubtitlesCodec(commandBuilder, fileQueueItem.getFirstFileFormat());
                }
            }
            commandBuilder.crf("30");
            commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
            commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);
            fFmpegHelper.addTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());

            fFmpegDevice.convert(file.getAbsolutePath(), result.getAbsolutePath(), commandBuilder.build());

            LOGGER.debug("Compress({}, {}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(),
                    fileQueueItem.getFirstFileFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), MemoryUtils.humanReadableByteCount(result.length()));

            if (fileQueueItem.getSize() <= result.length()) {
                Locale localeOrDefault = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INCOMPRESSIBLE_VIDEO, localeOrDefault)).setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            String compessionInfo = messageBuilder.getCompressionInfoMessage(fileQueueItem.getSize(), result.length(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), compessionInfo);
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem), compessionInfo);
            }
        } catch (Throwable e) {
            result.smartDelete();
            throw e;
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.FFmpegHelper;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
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

    private static final String SCALE = "scale=ceil(iw/2)*2:ceil(ih/2)*2";

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private ConversionMessageBuilder messageBuilder;

    @Autowired
    public VideoCompressConverter(FFmpegDevice fFmpegDevice, LocalisationService localisationService, UserService userService,
                                  FFprobeDevice fFprobeDevice, ConversionMessageBuilder messageBuilder) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.messageBuilder = messageBuilder;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegHelper.removeExtraVideoStreams(allStreams);

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            List<FFprobeDevice.Stream> videoStreams = allStreams.stream().filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())).collect(Collectors.toList());
            for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); videoStreamIndex++) {
                commandBuilder.mapVideo(videoStreamIndex);
                addVideoCodecOptions(commandBuilder, file, out, videoStreams.get(videoStreamIndex), videoStreamIndex);
                commandBuilder.filterVideo(videoStreamIndex, SCALE);
            }
            if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
                commandBuilder.mapAudio().copyAudio();
            }
            if (allStreams.stream().anyMatch(s -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))) {
                commandBuilder.mapSubtitles().copySubtitles();
            }
            commandBuilder.crf("30");
            commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
            commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);
            addOptionsBySrc(commandBuilder, fileQueueItem.getFirstFileFormat());

            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());

            LOGGER.debug("Compress({}, {}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(),
                    fileQueueItem.getFirstFileFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), MemoryUtils.humanReadableByteCount(out.length()));

            if (fileQueueItem.getSize() <= out.length()) {
                Locale localeOrDefault = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INCOMPRESSIBLE_VIDEO, localeOrDefault)).setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            String compessionInfo = messageBuilder.getCompressionInfoMessage(fileQueueItem.getSize(), out.length(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(out.getAbsolutePath(), 0);
                return new VideoResult(fileName, out, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), compessionInfo);
            } else {
                return new FileResult(fileName, out, downloadThumb(fileQueueItem), compessionInfo);
            }
        } catch (Throwable e) {
            out.smartDelete();
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

    private void addVideoCodecOptions(FFmpegCommandBuilder commandBuilder, SmartTempFile in, SmartTempFile out, FFprobeDevice.Stream videoStream,
                                      int videoStreamIndex) {
        if (StringUtils.isBlank(videoStream.getCodecName())) {
            return;
        }
        String codec = videoStream.getCodecName();
        if (!FFmpegCommandBuilder.H264_CODEC.equals(codec)) {
            if (isConvertiableToH264(in, out, videoStreamIndex)) {
                codec = FFmpegCommandBuilder.H264_CODEC;
            } else if (FFmpegCommandBuilder.VP9_CODEC.equals(codec)) {
                codec = FFmpegCommandBuilder.VP8_CODEC;
            }
        }

        commandBuilder.videoCodec(videoStreamIndex, codec);
    }

    private boolean isConvertiableToH264(SmartTempFile in, SmartTempFile out, int streamIndex) {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
        commandBuilder.mapVideo(streamIndex).videoCodec(FFmpegCommandBuilder.H264_CODEC).filterVideo(SCALE);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    private void addOptionsBySrc(FFmpegCommandBuilder commandBuilder, Format src) {
        if (src == _3GP) {
            commandBuilder.ar("8000").ba("12.20k").ac("1").s("176x144");
        }
    }
}

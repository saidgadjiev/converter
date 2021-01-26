package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.MessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
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
import java.util.stream.Stream;

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

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public VideoCompressConverter(FFmpegDevice fFmpegDevice, LocalisationService localisationService, UserService userService, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());

            String[] options = new String[]{"-c:a", "copy", "-c:s", "copy", "-vf",
                    "scale=-2:ceil(ih/3)*2", "-crf", "30", "-preset", "veryfast", "-deadline", "realtime", "-map",
                    "0", "-map", "-d", "-map", "-t"};
            String[] specificOptions = getOptionsBySrc(fileQueueItem.getFirstFileFormat());
            String[] allOptions = Stream.concat(Stream.of(specificOptions), Stream.of(options)).toArray(String[]::new);
            List<FFprobeDevice.Stream> videoStreams = allStreams.stream().filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())).collect(Collectors.toList());
            for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); videoStreamIndex++) {
                allOptions = Stream.concat(Stream.of(allOptions),
                        Stream.of(getOptionsByVideoStream(file, out, videoStreams.get(videoStreamIndex), videoStreamIndex))).toArray(String[]::new);
            }

            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), allOptions);

            LOGGER.debug("Compress({}, {}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(), fileQueueItem.getFirstFileId(),
                    fileQueueItem.getFirstFileFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), MemoryUtils.humanReadableByteCount(out.length()));

            if (fileQueueItem.getSize() <= out.length()) {
                Locale localeOrDefault = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INCOMPRESSIBLE_VIDEO, localeOrDefault)).setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());
            return new FileResult(fileName, out);
        } catch (Throwable e) {
            out.smartDelete();
            throw e;
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    private String[] getOptionsByVideoStream(SmartTempFile in, SmartTempFile out, FFprobeDevice.Stream videoStream,
                                             int videoStreamIndex) {
        if (StringUtils.isBlank(videoStream.getCodecName())) {
            return new String[0];
        }
        String codec = videoStream.getCodecName();
        if (isConvertiableToH264(in, out, videoStreamIndex)) {
            codec = "h264";
        } else if ("vp9".equals(codec)) {
            codec = "vp8";
        }

        return new String[]{
                "-c:v:" + videoStreamIndex, codec
        };
    }

    private boolean isConvertiableToH264(SmartTempFile in, SmartTempFile out, int streamIndex) {
        String[] options = new String[]{
                "-map", "v:" + streamIndex, "-c:v:" + streamIndex, "h264", "-vf", "[v:0]scale=ceil(iw/2)*2:ceil(ih/2)*2"
        };

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), options);
    }

    private String[] getOptionsBySrc(Format src) {
        if (src == _3GP) {
            return new String[]{
                    "-ar", "8000", "-b:a", "12.20k", "-ac", "1", "-s", "176x144"
            };
        }

        return new String[0];
    }
}

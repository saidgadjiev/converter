package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
public class FFmpegVideoFormatsConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegVideoFormatsConverter.class);

    private static final String TAG = "ffmpegvideo";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(MP4), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(_3GP), List.of(MP4, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(AVI), List.of(MP4, _3GP, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(FLV), List.of(MP4, _3GP, AVI, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(M4V), List.of(MP4, _3GP, AVI, FLV, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(MKV), List.of(MP4, _3GP, AVI, FLV, M4V, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(MOV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MPEG, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(MPEG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPG, MTS, VOB, WEBM, WMV));
        put(List.of(MPG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MTS, VOB, WEBM, WMV));
        put(List.of(MTS), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, VOB, WEBM, WMV));
        put(List.of(VOB), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, WEBM, WMV));
        put(List.of(WEBM), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV));
        put(List.of(WMV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV));
    }};

    private FFmpegDevice fFmpegDevice;

    private TempFileService fileService;

    private FileManager fileManager;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegVideoFormatsConverter(FFmpegDevice fFmpegDevice, TempFileService fileService,
                                       FileManager fileManager, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);

            SmartTempFile out = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                try {
                    String videoCodec = fFprobeDevice.getVideoCodec(file.getAbsolutePath());
                    fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), getCopyCodecsOptions(fileQueueItem.getFirstFileFormat(), videoCodec));
                } catch (ProcessException e) {
                    LOGGER.error("Error copy codecs({}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(),
                            fileQueueItem.getFirstFileId(), fileQueueItem.getFirstFileFormat(), fileQueueItem.getTargetFormat());
                    fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), getOptions(fileQueueItem.getFirstFileFormat(), fileQueueItem.getTargetFormat()));
                }

                SmartTempFile thumbFile = downloadThumb(fileQueueItem);
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, out, thumbFile);
            } catch (Throwable e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            file.smartDelete();
        }
    }

    private String[] getCopyCodecsOptions(Format target, String videoCodec) {
        if (target == AVI && Objects.equals(videoCodec, "h264")) {
            return new String[]{
                    "-bsf:v", "h264_mp4toannexb", "-c:v", "copy", "-c:a", "copy"
            };
        } else {
            return new String[]{
                    "-c:v", "copy", "-c:a", "copy"
            };
        }
    }

    private String[] getOptions(Format src, Format target) {
        if (src == VOB) {
            if (target == WEBM) {
                return new String[]{
                        "-c:v", "libvpx", "-deadline", "realtime", "-af", "aformat=channel_layouts=\"7.1|5.1|stereo\""
                };
            } else if (target == WMV) {
                return new String[]{
                        "-acodec", "copy", "-vcodec", "wmv2"
                };
            }
        }
        if (target == WEBM) {
            return new String[]{
                    "-c:v", "libvpx", "-deadline", "realtime"
            };
        }
        if (target == _3GP) {
            return new String[]{
                    "-vcodec", "h263", "-ar", "8000", "-b:a", "12.20k", "-ac", "1", "-s", "176x144"
            };
        }
        if (target == FLV) {
            return new String[]{
                    "-f", "flv", "-ar", "44100"
            };
        }
        if (target == MPEG) {
            return new String[]{
                    "-f", "dvd", "-filter:v", "scale='min(4095,iw)':min'(4095,ih)':force_original_aspect_ratio=decrease"
            };
        }
        if (target == MPG) {
            return new String[]{
                    "-f", "dvd"
            };
        }
        if (target == MTS) {
            return new String[]{
                    "-vcodec", "libx264", "-r", "30000/1001", "-b:v", "21M", "-acodec", "ac3"
            };
        }
        if (target == WMV) {
            return new String[]{
                    "-vcodec", "wmv2"
            };
        }
        return new String[0];
    }
}

package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.codec.VideoCodec;
import ru.gadjini.telegram.converter.service.conversion.codec.VideoCodecService;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
public class FFmpegFormatsConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegFormatsConverter.class);

    private static final String TAG = "ffmpeg";

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

    private FFprobeDevice fFprobeDevice;

    private TempFileService fileService;

    private FileManager fileManager;

    private VideoCodecService codecService;

    @Autowired
    public FFmpegFormatsConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice, TempFileService fileService,
                                  FileManager fileManager, VideoCodecService codecService) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.codecService = codecService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());

        try {
            Progress progress = progress(fileQueueItem.getUserId(), fileQueueItem);
            fileManager.downloadFileByFileId(fileQueueItem.getFirstFileId(), fileQueueItem.getSize(), progress, file);

            SmartTempFile out = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

            String videoCodecStr = StringUtils.defaultIfBlank(fFprobeDevice.getVideoCodec(file.getAbsolutePath()), "").replace("\n", "");
            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), getOptions(fileQueueItem.getId(), fileQueueItem.getSize(), fileQueueItem.getFirstFileFormat(), fileQueueItem.getTargetFormat(), videoCodecStr));

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, out);
        } finally {
            file.smartDelete();
        }
    }

    private String[] getOptions(int jobId, long fileSize, Format src, Format target, String videoCodecStr) {
        VideoCodec videoCodec = VideoCodec.fromCode(videoCodecStr);
        if (videoCodec == null) {
            LOGGER.debug("Unknown video codec({}, {}, {})", src, videoCodecStr, MemoryUtils.humanReadableByteCount(fileSize));
        }
        if (codecService.isVideoCodecSupported(target, videoCodec)) {
            LOGGER.debug("Copy codecs({}, {}, {}, {}, {})", jobId, src, target, videoCodecStr, MemoryUtils.humanReadableByteCount(fileSize));
            if (target == AVI && videoCodec == VideoCodec.H264) {
                return new String[]{
                        "-bsf:v", "h264_mp4toannexb", "-c:v", "copy", "-c:a", "copy"
                };
            } else {
                return new String[]{
                        "-c:v", "copy", "-c:a", "copy"
                };
            }
        } else {
            LOGGER.debug("Codecs not copied({}, {}, {}, {})", jobId, src, target, videoCodec);
        }
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

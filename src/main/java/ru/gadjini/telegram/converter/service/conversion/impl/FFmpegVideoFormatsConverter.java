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
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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
        put(List.of(MP4), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(_3GP), List.of(MP4, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(AVI), List.of(MP4, _3GP, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(FLV), List.of(MP4, _3GP, AVI, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(M4V), List.of(MP4, _3GP, AVI, FLV, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MKV), List.of(MP4, _3GP, AVI, FLV, M4V, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MOV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MPEG, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MPEG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MPG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MTS, VOB, WEBM, WMV, TS));
        put(List.of(MTS), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, VOB, WEBM, WMV, TS));
        put(List.of(VOB), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, WEBM, WMV, TS));
        put(List.of(WEBM), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, TS));
        put(List.of(WMV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, TS));
        put(List.of(TS), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, MP4));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegVideoFormatsConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
        String[] selectAllStreamsOptions = new String[]{"-map", "0", "-map", "-d", "-map", "-s", "-map", "-t"};
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            String[] allOptions = Stream.of(selectAllStreamsOptions).toArray(String[]::new);
            try {
                //Try to copy video audio codecs
                fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), Stream.concat(Stream.of(allOptions), Stream.of(getVideoAudioCodecsCopyOptions())).toArray(String[]::new));
            } catch (ProcessException e) {
                try {
                    //Try to copy video codecs
                    fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), Stream.concat(Stream.of(allOptions), Stream.of(getVideoCodecsCopyOptions())).toArray(String[]::new));
                } catch (ProcessException e1) {
                    LOGGER.debug("Error copy video codecs({}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getId(),
                            fileQueueItem.getFirstFileId(), fileQueueItem.getFirstFileFormat(), fileQueueItem.getTargetFormat());
                    String[] options = getOptions(fileQueueItem.getFirstFileFormat(), fileQueueItem.getTargetFormat());
                    allOptions = Stream.concat(Stream.of(options), Stream.of(allOptions)).toArray(String[]::new);

                    SmartTempFile subtitles = null;
                    try {
                        if (allStreams.stream().anyMatch(stream -> Objects.equals(stream.getCodecType(), FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE))) {
                            subtitles = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, SRT.getExt());
                            fFmpegDevice.convert(file.getAbsolutePath(), subtitles.getAbsolutePath());
                        }
                        if (subtitles != null) {
                            allOptions = Stream.concat(Stream.of(allOptions), Stream.of(getSubtitlesFilterOptions(subtitles.getAbsolutePath()))).toArray(String[]::new);
                        }
                        try {
                            //Try to copy audio codecs
                            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), Stream.concat(Stream.of(allOptions), Stream.of(getAudioCodecsCopyOptions())).toArray(String[]::new));
                        } catch (ProcessException e2) {
                            //Without copying
                            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), allOptions);
                        }
                    } catch (Throwable throwable) {
                        if (subtitles != null) {
                            subtitles.smartDelete();
                        }

                        throw throwable;
                    } finally {
                        if (subtitles != null) {
                            subtitles.smartDelete();
                        }
                    }
                }
            }

            SmartTempFile thumbFile = downloadThumb(fileQueueItem);
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, out, thumbFile);
        } catch (Throwable e) {
            out.smartDelete();
            throw e;
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    private String[] getSubtitlesFilterOptions(String subtitlesPath) {
        return new String[]{
                "-vf", "subtitles=" + subtitlesPath
        };
    }

    private String[] getVideoCodecsCopyOptions() {
        return new String[]{
                "-c:v", "copy"
        };
    }

    private String[] getAudioCodecsCopyOptions() {
        return new String[]{
                "-c:a", "copy"
        };
    }

    private String[] getVideoAudioCodecsCopyOptions() {
        return new String[]{
                "-c:v", "copy", "-c:a", "copy"
        };
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

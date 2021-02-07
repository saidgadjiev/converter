package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.FFmpegHelper;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
@SuppressWarnings("CPD-START")
public class FFmpegVideoFormatsConverter extends BaseAny2AnyConverter {

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
        String[] selectAllStreamsOptions = new String[]{"-preset", "veryfast", "-deadline", "realtime", "-map", "a", "-map", "-d", "-map", "-s", "-map", "-t"};
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegHelper.removeExtraVideoStreams(allStreams);
            String[] allOptions = Stream.concat(Stream.of(selectAllStreamsOptions),
                    Stream.of(getTargetFormatOptions(fileQueueItem.getTargetFormat()))).toArray(String[]::new);
            int audioStreamIndex = 0, videoStreamIndex = 0;
            for (FFprobeDevice.Stream stream : allStreams) {
                if (FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(stream.getCodecType())) {
                    allOptions = Stream.concat(Stream.of(allOptions), Stream.of("-map", "v:" + stream.getIndex())).toArray(String[]::new);
                    if (isCopyable(file, out, fileQueueItem.getTargetFormat(), "v", videoStreamIndex)) {
                        allOptions = Stream.concat(Stream.of(allOptions), Stream.of("-c:v:" + videoStreamIndex, "copy")).toArray(String[]::new);
                    } else {
                        allOptions = Stream.concat(Stream.of(allOptions),
                                Stream.of(getVideoCodecOptions(fileQueueItem.getTargetFormat(), videoStreamIndex))).toArray(String[]::new);
                    }
                    ++videoStreamIndex;
                } else if (FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType())) {
                    if (isCopyable(file, out, fileQueueItem.getTargetFormat(), "a", audioStreamIndex)) {
                        allOptions = Stream.concat(Stream.of(allOptions), Stream.of("-c:a:" + audioStreamIndex, "copy")).toArray(String[]::new);
                    } else {
                        allOptions = Stream.concat(Stream.of(allOptions),
                                Stream.of(getAudioCodecOptions(fileQueueItem.getTargetFormat(), audioStreamIndex))).toArray(String[]::new);
                    }
                    ++audioStreamIndex;
                }
            }
            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), allOptions);

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());

            if (fileQueueItem.getTargetFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(out.getAbsolutePath(), 0);

                return new VideoResult(fileName, out, fileQueueItem.getTargetFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), fileQueueItem.getTargetFormat().supportsStreaming());
            } else {
                return new FileResult(fileName, out, downloadThumb(fileQueueItem));
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

    private boolean isCopyable(SmartTempFile in, SmartTempFile out, Format targetFormat, String streamPrefix, int streamIndex) {
        String[] options = new String[]{
                "-map", streamPrefix + ":" + streamIndex, "-c:" + streamPrefix, "copy"
        };
        options = Stream.concat(Stream.of(options), Stream.of(getTargetFormatOptions(targetFormat))).toArray(String[]::new);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), options);
    }

    private String[] getTargetFormatOptions(Format target) {
        if (target == MPEG || target == MPG) {
            return new String[]{
                    "-f", "mpegts"
            };
        }
        if (target == _3GP) {
            return new String[]{
                    "-ar", "8000", "-b:a", "12.20k", "-ac", "1", "-s", "176x144"
            };
        }
        if (target == FLV) {
            return new String[]{
                    "-f", "flv", "-ar", "44100"
            };
        }
        if (target == MTS) {
            return new String[]{
                    "-r", "30000/1001"
            };
        }

        return new String[0];
    }

    private String[] getAudioCodecOptions(Format target, int streamIndex) {
        if (target == MTS) {
            return new String[]{
                    "-c:a:" + streamIndex, "ac3"
            };
        }

        return new String[0];
    }

    private String[] getVideoCodecOptions(Format target, int streamIndex) {
        if (target == WEBM) {
            return new String[]{
                    "-c:v:" + streamIndex, "vp8"
            };
        }
        if (target == _3GP) {
            return new String[]{
                    "-c:v:" + streamIndex, "h263"
            };
        }
        if (target == MTS) {
            return new String[]{
                    "-c:v:" + streamIndex, "h264", "-b:v:" + streamIndex, "21M"
            };
        }
        if (target == WMV) {
            return new String[]{
                    "-c:v:" + streamIndex, "wmv2"
            };
        }

        return new String[0];
    }
}

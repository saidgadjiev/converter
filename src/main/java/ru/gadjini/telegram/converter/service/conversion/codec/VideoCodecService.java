package ru.gadjini.telegram.converter.service.conversion.codec;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.converter.service.conversion.codec.VideoCodec.*;

@Service
public class VideoCodecService {

    private static final Map<Format, List<VideoCodec>> CODECS = new HashMap<>();

    static {
        CODECS.put(Format._3GP, List.of(H263, H264, MPEG4));
        CODECS.put(Format.AVI, List.of(MPEG4, H264, H265, MJPEG, DV, MPEG1, MPEG2, VC1, H263, CINEPAK, VP8));
        CODECS.put(Format.FLV, List.of(FLV, MPEG4));
        CODECS.put(Format.MTS, List.of(MPEG2, H264, H265));
        CODECS.put(Format.MKV, List.of(H264, H265, MPEG4, MPEG1, MPEG2, MJPEG, THEORA, DV, VP9, SVQ1, SVQ3, CINEPAK, VP8));
        CODECS.put(Format.MOV, List.of(H264, MPEG4, MJPEG, PNG));
        CODECS.put(Format.MPG, List.of(MPEG1, MPEG2));
        CODECS.put(Format.MPEG, List.of(MPEG1, MPEG2));
        CODECS.put(Format.VOB, List.of(MPEG1, MPEG2));
        CODECS.put(Format.WEBM, List.of(VP7, VP8, VP9));
        CODECS.put(Format.WMV, List.of(MPEG4));
        CODECS.put(Format.MP4, List.of(MJPEG, MPEG2, VC1, SVQ1, SVQ3, CINEPAK, THEORA, MPEG4, H263, H264, H265, DIRAC, VP8, VP9, AV1));
        CODECS.put(Format.M4V, List.of(MJPEG, MPEG4, H263, H264, H265));
    }

    public boolean isVideoCodecSupported(Format format, VideoCodec codec) {
        if (codec == null) {
            return false;
        }

        return CODECS.containsKey(format) && CODECS.get(format).contains(codec);
    }
}

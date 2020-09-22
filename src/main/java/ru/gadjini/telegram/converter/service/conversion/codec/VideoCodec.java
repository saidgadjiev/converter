package ru.gadjini.telegram.converter.service.conversion.codec;

import org.apache.commons.lang3.StringUtils;

public enum VideoCodec {

    H264("h264"),
    H263("h263"),
    MPEG4("mpeg4"),
    MJPEG("mjpeg"),
    DV("dvvideo"),
    FRAPS("fraps"),
    TSCC("tscc"),
    MPEG1("mpeg1video"),
    MPEG2("mpeg2video"),
    FLV("flv1"),
    H265("hevc"),
    THEORA("theora"),
    VP9("vp9"),
    PRORES("prores"),
    PNG("png"),
    VP7("vp7"),
    VP8("vp8"),
    VC1("vc1"),
    CINEPAK("cinepak"),
    INDEO2("indeo2"),
    INDEO3("indeo3"),
    INDEO4("indeo4"),
    INDEO5("indeo5"),
    VP6("vp6"),
    SVQ1("svq1"),
    SVQ3("svq3"),
    DIRAC("dirac"),
    AV1("av1");

    private final String code;

    VideoCodec(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static VideoCodec fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (VideoCodec videoCodec: values()) {
            if (videoCodec.code.equals(code)) {
                return videoCodec;
            }
        }

        return null;
    }
}

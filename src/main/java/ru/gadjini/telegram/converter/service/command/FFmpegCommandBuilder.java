package ru.gadjini.telegram.converter.service.command;

import java.util.ArrayList;
import java.util.List;

public class FFmpegCommandBuilder {

    public static final String DEFAULT_CRF = "26";

    public static final String CRF = "-crf";

    public static final String EVEN_SCALE = "scale=-2:ceil(ih/2)*2";

    public static final String _3GP_SCALE = "scale=176:144";

    public static final String PRESET_VERY_FAST = "veryfast";

    public static final String DEADLINE_REALTIME = "realtime";

    public static final String TUNE_STILLIMAGE = "stillimage";

    public static final String LIBMP3LAME = "libmp3lame";

    public static final String MP3 = "mp3";

    public static final String YUV_420_P = "yuv420p";

    public static final String VIDEO_STREAM_SPECIFIER = "v";

    public static final String AUDIO_STREAM_SPECIFIER = "a";

    public static final String H264_CODEC = "h264";

    public static final String H263_CODEC = "h263";

    public static final String VP8_CODEC = "vp8";

    public static final String VP9_CODEC = "vp9";

    public static final String AV1_CODEC = "av1";

    public static final String MPEGTS_FORMAT = "mpegts";

    public static final String MJPEG = "mjpeg";

    public static final String BMP = "bmp";

    public static final String PNG = "png";

    public static final String FLV_FORMAT = "flv";

    public static final String AC3_CODEC = "ac3";

    public static final String AAC_CODEC = "aac";

    public static final String WMV2 = "wmv2";

    public static final String VORBIS = "libvorbis";

    public static final String MOV_TEXT_CODEC = "mov_text";

    public static final String WEBVTT_CODEC = "webvtt";

    public static final String SRT_CODEC = "srt";

    private List<String> options = new ArrayList<>();

    private static final List<String> DEFAULT_OPTIONS = List.of("-max_muxing_queue_size", "9999", "-pix_fmt", YUV_420_P);

    public FFmpegCommandBuilder(FFmpegCommandBuilder commandBuilder) {
        this.options.addAll(commandBuilder.options);
    }

    public FFmpegCommandBuilder() {
    }

    public FFmpegCommandBuilder hideBanner() {
        options.add("-hide_banner");

        return this;
    }

    public FFmpegCommandBuilder loop(int loop) {
        options.add("-loop");
        options.add(String.valueOf(loop));

        return this;
    }

    public FFmpegCommandBuilder quite() {
        options.add("-y");

        return this;
    }

    public FFmpegCommandBuilder ss(String startPoint) {
        options.add("-ss");
        options.add(startPoint);

        return this;
    }

    public FFmpegCommandBuilder to(String to) {
        options.add("-to");
        options.add(to);

        return this;
    }

    public FFmpegCommandBuilder t(String duration) {
        options.add("-t");
        options.add(duration);

        return this;
    }

    public FFmpegCommandBuilder input(String filePath) {
        options.add("-i");
        options.add(filePath);

        return this;
    }

    public FFmpegCommandBuilder tune(String tune) {
        options.add("-tune");
        options.add(tune);

        return this;
    }

    public FFmpegCommandBuilder shortest() {
        options.add("-shortest");

        return this;
    }

    public FFmpegCommandBuilder t(long seconds) {
        options.add("-t");
        options.add(String.valueOf(seconds));

        return this;
    }

    public FFmpegCommandBuilder out(String filePath) {
        options.add(filePath);

        return this;
    }

    public FFmpegCommandBuilder map(String streamSpecifier, int index) {
        options.add("-map");
        options.add(streamSpecifier + ":" + index);

        return this;
    }

    public FFmpegCommandBuilder copy(String streamSpecifier) {
        options.add("-c:" + streamSpecifier);
        options.add("copy");

        return this;
    }

    public FFmpegCommandBuilder copySubtitles() {
        options.add("-c:s");
        options.add("copy");

        return this;
    }

    public FFmpegCommandBuilder copySubtitles(int index) {
        options.add("-c:s:" + index);
        options.add("copy");

        return this;
    }

    public FFmpegCommandBuilder subtitlesCodec(String codec) {
        options.add("-c:s");
        options.add(codec);

        return this;
    }

    public FFmpegCommandBuilder subtitlesCodec(String codec, int index) {
        options.add("-c:s:" + index);
        options.add(codec);

        return this;
    }

    public FFmpegCommandBuilder mapSubtitles() {
        options.add("-map");
        options.add("s");

        return this;
    }

    public FFmpegCommandBuilder mapSubtitlesInput(Integer input) {
        options.add("-map");
        if (input == null) {
            options.add("s");
        } else {
            options.add(input + ":s");
        }

        return this;
    }

    public FFmpegCommandBuilder mapSubtitles(int index) {
        options.add("-map");
        options.add("s:" + index);

        return this;
    }

    public FFmpegCommandBuilder mapSubtitles(Integer input, int index) {
        options.add("-map");
        if (input == null) {
            options.add("s:" + index);
        } else {
            options.add(index + ":s:" + index);
        }

        return this;
    }

    public FFmpegCommandBuilder copyAudio() {
        options.add("-c:a");
        options.add("copy");

        return this;
    }

    public FFmpegCommandBuilder copyAudio(int index) {
        options.add("-c:a:" + index);
        options.add("copy");

        return this;
    }

    public FFmpegCommandBuilder audioCodec(int index, String codec) {
        options.add("-c:a:" + index);
        options.add(codec);

        return this;
    }

    public FFmpegCommandBuilder audioCodec(String codec) {
        options.add("-c:a");
        options.add(codec);

        return this;
    }

    public FFmpegCommandBuilder qa(String qa) {
        options.add("-q:a");
        options.add(qa);

        return this;
    }

    public FFmpegCommandBuilder mapAudio() {
        options.add("-map");
        options.add("a");

        return this;
    }

    public FFmpegCommandBuilder mapAudioInput(Integer input) {
        options.add("-map");
        if (input == null) {
            options.add("a");
        } else {
            options.add(input + ":a");
        }

        return this;
    }

    public FFmpegCommandBuilder mapAudio(int index) {
        options.add("-map");
        options.add("a:" + index);

        return this;
    }

    public FFmpegCommandBuilder mapAudio(Integer input, int index) {
        options.add("-map");
        if (input == null) {
            options.add("a:" + index);
        } else {
            options.add(input + ":a:" + index);
        }

        return this;
    }

    public FFmpegCommandBuilder mapVideo(int index) {
        options.add("-map");
        options.add("v:" + index);

        return this;
    }

    public FFmpegCommandBuilder mapVideo(Integer input, int index) {
        options.add("-map");
        if (input == null) {
            options.add("v:" + index);
        } else {
            options.add(input + ":v:" + index);
        }

        return this;
    }

    public FFmpegCommandBuilder mapVideo() {
        options.add("-map");
        options.add("v");

        return this;
    }

    public FFmpegCommandBuilder videoCodec(int index, String codec) {
        options.add("-c:v:" + index);
        options.add(codec);

        return this;
    }


    public FFmpegCommandBuilder videoCodec(String codec) {
        options.add("-c:v");
        options.add(codec);

        return this;
    }

    public FFmpegCommandBuilder copyVideo(int index) {
        options.add("-c:v:" + index);
        options.add("copy");

        return this;
    }

    public FFmpegCommandBuilder filterVideo(int index, String filter) {
        options.add("-filter:v:" + index);
        options.add(filter);

        return this;
    }


    public FFmpegCommandBuilder filterVideo(String filter) {
        options.add("-vf");
        options.add(filter);

        return this;
    }

    public FFmpegCommandBuilder preset(String preset) {
        options.add("-preset");
        options.add(preset);

        return this;
    }

    public FFmpegCommandBuilder deadline(String deadline) {
        options.add("-deadline");
        options.add(deadline);

        return this;
    }

    public FFmpegCommandBuilder crf(String crf) {
        options.add(CRF);
        options.add(crf);

        return this;
    }

    public FFmpegCommandBuilder ba(String ba) {
        options.add("-b:a");
        options.add(ba);

        return this;
    }

    public FFmpegCommandBuilder r(String r) {
        options.add("-r");
        options.add(r);

        return this;
    }

    public FFmpegCommandBuilder framerate(String framerate) {
        options.add("-framerate");
        options.add(framerate);

        return this;
    }

    public FFmpegCommandBuilder bv(int index, String bv) {
        options.add("-b:v:" + index);
        options.add(bv);

        return this;
    }

    public FFmpegCommandBuilder ac(String ac) {
        options.add("-ac");
        options.add(ac);

        return this;
    }

    public FFmpegCommandBuilder s(String s) {
        options.add("-s");
        options.add(s);

        return this;
    }

    public FFmpegCommandBuilder f(String format) {
        options.add("-f");
        options.add(format);

        return this;
    }

    public FFmpegCommandBuilder ar(String ar) {
        options.add("-ar");
        options.add(ar);

        return this;
    }

    public FFmpegCommandBuilder an() {
        options.add("-an");

        return this;
    }

    public FFmpegCommandBuilder defaultOptions() {
        List<String> def = new ArrayList<>();
        if (!options.contains(CRF)) {
            def.add(CRF);
            def.add(DEFAULT_CRF);
        }
        options.addAll(def);
        options.addAll(DEFAULT_OPTIONS);

        return this;
    }

    public String[] build() {
        return options.toArray(new String[0]);
    }

    public String[] buildFullCommand() {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.addAll(options);

        return command.toArray(new String[0]);
    }
}

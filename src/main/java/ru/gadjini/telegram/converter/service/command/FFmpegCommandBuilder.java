package ru.gadjini.telegram.converter.service.command;

import java.util.ArrayList;
import java.util.List;

public class FFmpegCommandBuilder {

    public static final String EVEN_SCALE = "scale=-2:ceil(ih/2)*2";

    public static final String PRESET_VERY_FAST = "veryfast";

    public static final String DEADLINE_REALTIME = "realtime";

    public static final String VIDEO_STREAM_SPECIFIER = "v";

    public static final String AUDIO_STREAM_SPECIFIER = "a";

    public static final String H264_CODEC = "h264";

    public static final String H263_CODEC = "h263";

    public static final String VP8_CODEC = "vp8";

    public static final String VP9_CODEC = "vp9";

    public static final String MPEGTS_FORMAT = "mpegts";

    public static final String FLV_FORMAT = "flv";

    public static final String AC3_CODEC = "ac3";

    public static final String WMV2 = "wmv2";

    public static final String MOV_TEXT_CODEC = "mov_text";

    public static final String WEBVTT_CODEC = "webvtt";

    public static final String SRT_CODEC = "srt";

    private List<String> options = new ArrayList<>();

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

    public FFmpegCommandBuilder subtitlesCodec(String codec) {
        options.add("-c:s");
        options.add(codec);

        return this;
    }

    public FFmpegCommandBuilder mapSubtitles() {
        options.add("-map");
        options.add("s");

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

    public FFmpegCommandBuilder mapAudio() {
        options.add("-map");
        options.add("a");

        return this;
    }

    public FFmpegCommandBuilder mapVideo(int index) {
        options.add("-map");
        options.add("v:" + index);

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
        options.add("-crf");
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

    public String[] build() {
        return options.toArray(new String[0]);
    }
}

package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FFmpegCommandBuilder {

    private static final int DEFAULT_AUDIO_BIT_RATE = 128;

    public static final String CRF = "-crf";

    public static final String CONCAT = "concat";

    public static final String EVEN_SCALE = "scale=-2:ceil(ih/2)*2";

    public static final String _3GP_SCALE = "scale=176:144";

    public static final String H263_PLUS_SCALE = "scale='min(2048,iw)':min'(1152,ih)'";

    public static final String H263_PLUS_CODEC = "h263p";

    public static final String PRESET_VERY_FAST = "veryfast";

    public static final String OPUS = "libopus";

    public static final String OPUS_CODEC_NAME = "opus";

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

    public static final String LIBVORBIS = "libvorbis";

    public static final String AAC_CODEC = "aac";

    public static final String WMV2 = "wmv2";

    public static final String VORBIS = "libvorbis";

    public static final String MOV_TEXT_CODEC = "mov_text";

    public static final String WEBVTT_CODEC = "webvtt";

    public static final String SRT_CODEC = "srt";

    private List<String> options = new ArrayList<>();

    private static final List<String> DEFAULT_OPTIONS = List.of("-strict", "-2",
            "-vsync", "2", "-max_muxing_queue_size", "9999", "-pix_fmt", YUV_420_P);

    private List<String> complexFilters = new ArrayList<>();

    private boolean useFilterComplex;

    public FFmpegCommandBuilder(FFmpegCommandBuilder commandBuilder) {
        this.options.addAll(commandBuilder.options);
    }

    public FFmpegCommandBuilder() {
    }

    public FFmpegCommandBuilder useFilterComplex(boolean useFilterComplex) {
        this.useFilterComplex = useFilterComplex;

        return this;
    }

    public boolean useFilterComplex() {
        return useFilterComplex;
    }

    public FFmpegCommandBuilder safe(String s) {
        options.add("-safe");
        options.add(s);

        return this;
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
            options.add(input + ":s:" + index);
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

    public FFmpegCommandBuilder audioBitrate(long bitrate) {
        options.add("-b:a");
        options.add(bitrate + "k");

        return this;
    }

    public FFmpegCommandBuilder audioCodec(String codec) {
        options.add("-c:a");
        options.add(codec);

        return this;
    }

    public FFmpegCommandBuilder copyCodecs() {
        options.add("-c");
        options.add("copy");

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

    public FFmpegCommandBuilder copyVideo() {
        options.add("-c:v");
        options.add("copy");

        return this;
    }

    public FFmpegCommandBuilder filterVideo(int index, String filter) {
        options.add("-filter:v:" + index);
        options.add(filter);

        return this;
    }

    public FFmpegCommandBuilder complexFilter(String filter) {
        complexFilters.add(filter);

        return this;
    }

    public List<String> getComplexFilters() {
        return complexFilters;
    }

    public FFmpegCommandBuilder filterAudio(String filter) {
        options.add("-af");
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

    public FFmpegCommandBuilder qmin(String qmin) {
        options.add("-qmin");
        options.add(qmin);

        return this;
    }

    public FFmpegCommandBuilder qmax(String qmax) {
        options.add("-qmax");
        options.add(qmax);

        return this;
    }

    public FFmpegCommandBuilder vp8QualityOptions() {
        qmin("0").qmax("40");

        if (!options.contains(CRF)) {
            crf("5");
        }

        if (options.stream().noneMatch(o -> o.contains("-b:v"))) {
            bv("1M");
            options.add("-maxrate");
            options.add("1M");
            options.add("-bufsize");
            options.add("1.5M");
        }

        return this;
    }

    public FFmpegCommandBuilder fastConversion() {
        preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
        speed("16");

        return this;
    }

    public FFmpegCommandBuilder deadline(String deadline) {
        options.add("-deadline");
        options.add(deadline);

        return this;
    }

    public FFmpegCommandBuilder speed(String speed) {
        options.add("-speed");
        options.add(speed);

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

    public FFmpegCommandBuilder ba(int index, String ba) {
        options.add("-b:a:" + index);
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

    public FFmpegCommandBuilder bv(String bv) {
        options.add("-b:v");
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

    public FFmpegCommandBuilder q(String streamSpecifier, String q) {
        options.add("-q:" + streamSpecifier);
        options.add(q);

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

    public FFmpegCommandBuilder keepVideoBitRate(int index, long fileSize, Long duration, List<FFprobeDevice.Stream> allStreams) {
        if (fileSize == 0 || fileSize < 0) {
            return this;
        }
        if (duration == null) {
            return this;
        }
        List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                .filter(f -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(f.getCodecType()))
                .collect(Collectors.toList());
        FFprobeDevice.Stream firstAudioStream = audioStreams.stream().findFirst().orElse(null);
        long bitRate;
        if (firstAudioStream == null) {
            bitRate = calculateBitRate(fileSize, duration);
        } else {
            bitRate = calculateBitRate(fileSize, duration, firstAudioStream.getBitRate() == null ? DEFAULT_AUDIO_BIT_RATE : firstAudioStream.getBitRate() / 1000);
        }
        bv(index, bitRate + "k");
        if (!options.contains("-maxrate")) {
            options.add("-maxrate");
            options.add(bitRate + "k");
            options.add("-bufsize");
            options.add((long) (bitRate * 1.5) + "k");
        }

        return this;
    }

    public FFmpegCommandBuilder defaultOptions() {
        options.addAll(DEFAULT_OPTIONS);

        return this;
    }

    public FFmpegCommandBuilder complexFilters() {
        options.addAll(getComplexFilterOptions());

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

    private List<String> getComplexFilterOptions() {
        List<String> options = new ArrayList<>();
        if (useFilterComplex && !complexFilters.isEmpty()) {
            options.add("-filter_complex");
            String complexFilter = String.join(";", complexFilters);
            options.add(complexFilter);
        }

        return options;
    }

    private long calculateBitRate(long fileSize, long duration) {
        return (fileSize / 1024 * 8) / duration;
    }

    private long calculateBitRate(long fileSize, long duration, long audioBitRate) {
        long videoBitRate = (fileSize / 1024 * 8) / duration;

        return audioBitRate >= videoBitRate ? videoBitRate : videoBitRate - audioBitRate;
    }
}

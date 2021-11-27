package ru.gadjini.telegram.converter.service.command;

import org.apache.commons.lang3.StringUtils;
import ru.gadjini.telegram.converter.utils.BitrateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FFmpegCommand {

    public static final String CRF = "-crf";

    public static final String CONCAT = "concat";

    public static final String EVEN_SCALE = "scale=-2:ceil(ih/2)*2";

    public static final String _3GP_SCALE = "scale=176:144";

    public static final String PRESET_VERY_FAST = "veryfast";

    public static final String PAM_CODEC = "pam";

    public static final String LIBOPUS = "libopus";

    public static final String OPUS_CODEC_NAME = "opus";

    public static final String DEADLINE_REALTIME = "realtime";

    public static final String TUNE_STILLIMAGE = "stillimage";

    public static final String LIBMP3LAME = "libmp3lame";

    public static final String MP3 = "mp3";

    public static final String YUV_420_P = "yuv420p";

    public static final String VIDEO_STREAM_SPECIFIER = "v";

    public static final String SUBTITLES_STREAM_SPECIFIER = "s";

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

    private List<String> complexFilters = new ArrayList<>();

    private boolean useFilterComplex;

    public FFmpegCommand(FFmpegCommand commandBuilder) {
        this.options.addAll(commandBuilder.options);
    }

    public FFmpegCommand() {
    }

    public FFmpegCommand useFilterComplex(boolean useFilterComplex) {
        this.useFilterComplex = useFilterComplex;

        return this;
    }

    public boolean useFilterComplex() {
        return useFilterComplex;
    }

    public boolean hasAudioFilter() {
        return options.contains("-af");
    }

    public FFmpegCommand maxMuxingQueueSize(String size) {
        options.add("-max_muxing_queue_size");
        options.add(size);

        return this;
    }

    public FFmpegCommand vsync(String vsync) {
        options.add("-vsync");
        options.add(vsync);

        return this;
    }

    public FFmpegCommand strict(String strict) {
        options.add("-strict");
        options.add(strict);

        return this;
    }

    public FFmpegCommand pixFmt(String pixFmt) {
        options.add("-pix_fmt");
        options.add(pixFmt);

        return this;
    }


    public FFmpegCommand safe(String s) {
        options.add("-safe");
        options.add(s);

        return this;
    }

    public FFmpegCommand hideBanner() {
        options.add("-hide_banner");

        return this;
    }

    public FFmpegCommand loop(int loop) {
        options.add("-loop");
        options.add(String.valueOf(loop));

        return this;
    }

    public FFmpegCommand quite() {
        options.add("-y");

        return this;
    }

    public FFmpegCommand segmentTimes(List<Long> segmentTimes) {
        options.add("-f");
        options.add("segment");
        options.add("-segment_times");
        options.add(segmentTimes.stream().map(String::valueOf).collect(Collectors.joining(",")));

        return this;
    }

    public FFmpegCommand ss(String startPoint) {
        options.add("-ss");
        options.add(startPoint);

        return this;
    }

    public FFmpegCommand to(String to) {
        options.add("-to");
        options.add(to);

        return this;
    }

    public FFmpegCommand t(String duration) {
        options.add("-t");
        options.add(duration);

        return this;
    }

    public FFmpegCommand vframes(String frames) {
        options.add("-vframes");
        options.add(frames);

        return this;
    }

    public FFmpegCommand qv(String qv) {
        options.add("-q:v");
        options.add(qv);

        return this;
    }

    public FFmpegCommand input(String filePath) {
        options.add("-i");
        options.add(filePath);

        return this;
    }

    public FFmpegCommand ignoreLoop() {
        options.add("-ignore_loop");
        options.add("0");

        return this;
    }

    public FFmpegCommand tune(String tune) {
        options.add("-tune");
        options.add(tune);

        return this;
    }

    public FFmpegCommand shortest() {
        options.add("-shortest");

        return this;
    }

    public FFmpegCommand t(long seconds) {
        options.add("-t");
        options.add(String.valueOf(seconds));

        return this;
    }

    public FFmpegCommand out(String filePath) {
        options.add(filePath);

        return this;
    }

    public FFmpegCommand map(String streamSpecifier, int index) {
        options.add("-map");
        options.add(streamSpecifier + ":" + index);

        return this;
    }

    public FFmpegCommand copy(String streamSpecifier) {
        options.add("-c:" + streamSpecifier);
        options.add("copy");

        return this;
    }

    public FFmpegCommand copySubtitles() {
        options.add("-c:s");
        options.add("copy");

        return this;
    }

    public FFmpegCommand copySubtitles(int index) {
        options.add("-c:s:" + index);
        options.add("copy");

        return this;
    }

    public FFmpegCommand subtitlesCodec(String codec) {
        options.add("-c:s");
        options.add(codec);

        return this;
    }

    public FFmpegCommand subtitlesCodec(String codec, int index) {
        options.add("-c:s:" + index);
        options.add(codec);

        return this;
    }

    public FFmpegCommand mapSubtitles() {
        options.add("-map");
        options.add("s");

        return this;
    }

    public FFmpegCommand mapSubtitlesInput(Integer input) {
        options.add("-map");
        if (input == null) {
            options.add("s");
        } else {
            options.add(input + ":s");
        }

        return this;
    }

    public FFmpegCommand mapSubtitles(int index) {
        options.add("-map");
        options.add("s:" + index);

        return this;
    }

    public FFmpegCommand mapSubtitles(Integer input, int index) {
        options.add("-map");
        if (input == null) {
            options.add("s:" + index);
        } else {
            options.add(input + ":s:" + index);
        }

        return this;
    }

    public FFmpegCommand copyAudio() {
        options.add("-c:a");
        options.add("copy");

        return this;
    }

    public FFmpegCommand copyAudio(int index) {
        options.add("-c:a:" + index);
        options.add("copy");

        return this;
    }

    public FFmpegCommand audioCodec(int index, String codec) {
        options.add("-c:a:" + index);
        options.add(codec);

        return this;
    }

    public FFmpegCommand audioBitrate(long bitrate) {
        options.add("-b:a");
        options.add(bitrate + "k");

        return this;
    }

    public FFmpegCommand audioCodec(String codec) {
        options.add("-c:a");
        options.add(codec);

        return this;
    }

    public FFmpegCommand copyCodecs() {
        options.add("-c");
        options.add("copy");

        return this;
    }

    public FFmpegCommand qa(String qa) {
        options.add("-q:a");
        options.add(qa);

        return this;
    }

    public FFmpegCommand mapAudio() {
        options.add("-map");
        options.add("a");

        return this;
    }

    public FFmpegCommand mapAudioInput(Integer input) {
        options.add("-map");
        if (input == null) {
            options.add("a");
        } else {
            options.add(input + ":a");
        }

        return this;
    }

    public FFmpegCommand mapAudio(int index) {
        options.add("-map");
        options.add("a:" + index);

        return this;
    }

    public FFmpegCommand mapAudio(Integer input, int index) {
        options.add("-map");
        if (input == null) {
            options.add("a:" + index);
        } else {
            options.add(input + ":a:" + index);
        }

        return this;
    }

    public FFmpegCommand mapVideo(int index) {
        options.add("-map");
        options.add("v:" + index);

        return this;
    }

    public FFmpegCommand mapVideo(Integer input, int index) {
        options.add("-map");
        if (input == null) {
            options.add("v:" + index);
        } else {
            options.add(input + ":v:" + index);
        }

        return this;
    }

    public FFmpegCommand mapVideo() {
        options.add("-map");
        options.add("v");

        return this;
    }

    public FFmpegCommand videoCodec(int index, String codec) {
        options.add("-c:v:" + index);
        options.add(codec);

        return this;
    }


    public FFmpegCommand videoCodec(String codec) {
        options.add("-c:v");
        options.add(codec);

        return this;
    }

    public FFmpegCommand copyVideo(int index) {
        options.add("-c:v:" + index);
        options.add("copy");

        return this;
    }

    public FFmpegCommand copyVideo() {
        options.add("-c:v");
        options.add("copy");

        return this;
    }

    public FFmpegCommand vf(String filter) {
        options.add("-vf");
        options.add(filter);

        return this;
    }

    public FFmpegCommand filterVideo(int index, String filter) {
        if (StringUtils.isBlank(filter)) {
            return this;
        }
        options.add("-filter:v:" + index);
        options.add(filter);

        return this;
    }

    public FFmpegCommand complexFilter(String filter) {
        complexFilters.add(filter);

        return this;
    }

    public List<String> getComplexFilters() {
        return complexFilters;
    }

    public FFmpegCommand filterAudio(String filter) {
        options.add("-af");
        options.add(filter);

        return this;
    }

    public FFmpegCommand filterAudioV2(String filter) {
        options.add("-filter:a");
        options.add(filter);

        return this;
    }

    public FFmpegCommand filterVideo(String filter) {
        options.add("-vf");
        options.add(filter);

        return this;
    }

    public FFmpegCommand preset(String preset) {
        options.add("-preset");
        options.add(preset);

        return this;
    }

    public FFmpegCommand qmin(String qmin) {
        options.add("-qmin");
        options.add(qmin);

        return this;
    }

    public FFmpegCommand qmax(String qmax) {
        options.add("-qmax");
        options.add(qmax);

        return this;
    }

    public FFmpegCommand speed(String speed) {
        options.add("-speed");
        options.add(speed);

        return this;
    }

    public FFmpegCommand crf(String crf) {
        options.add(CRF);
        options.add(crf);

        return this;
    }

    public FFmpegCommand ba(int index, String ba) {
        options.add("-b:a:" + index);
        options.add(ba);

        return this;
    }

    public FFmpegCommand ba(String ba) {
        options.add("-b:a");
        options.add(ba);

        return this;
    }

    public FFmpegCommand af(String filter) {
        options.add("-af");
        options.add(filter);

        return this;
    }

    public FFmpegCommand r(String r) {
        options.add("-r");
        options.add(r);

        return this;
    }

    public FFmpegCommand framerate(String framerate) {
        options.add("-framerate");
        options.add(framerate);

        return this;
    }

    public FFmpegCommand bv(int index, String bv) {
        options.add("-b:v:" + index);
        options.add(bv);

        return this;
    }

    public FFmpegCommand bv(String bv) {
        options.add("-b:v");
        options.add(bv);

        return this;
    }

    public boolean hasAc() {
        return options.stream().anyMatch(a -> a.contains("-ac"));
    }

    public boolean hasChannelLayoutFilter() {
        return options.stream().anyMatch(a -> a.contains("channelmap=channel_layout"));
    }

    public FFmpegCommand ac(String ac) {
        options.add("-ac");
        options.add(ac);

        return this;
    }

    public FFmpegCommand s(String s) {
        options.add("-s");
        options.add(s);

        return this;
    }

    public FFmpegCommand streamOut() {
        options.add("-");

        return this;
    }

    public FFmpegCommand f(String format) {
        options.add("-f");
        options.add(format);

        return this;
    }

    public FFmpegCommand q(String streamSpecifier, String q) {
        options.add("-q:" + streamSpecifier);
        options.add(q);

        return this;
    }

    public FFmpegCommand ar(String ar) {
        options.add("-ar");
        options.add(ar);

        return this;
    }

    public FFmpegCommand an() {
        options.add("-an");

        return this;
    }

    public FFmpegCommand keepAudioBitRate(int index, Integer bitRate) {
        if (bitRate == null) {
            return this;
        }

        ba(index, BitrateUtils.toKBytes(bitRate) + "k");

        return this;
    }

    public FFmpegCommand keepAudioBitRate(Integer bitRate) {
        if (bitRate == null) {
            return this;
        }

        ba(BitrateUtils.toKBytes(bitRate) + "k");

        return this;
    }

    public FFmpegCommand keepVideoBitRate(int index, Integer videoBitRate) {
        return keepVideoBitRate(videoBitRate, index, 100);
    }

    public FFmpegCommand keepVideoBitRate(Integer videoBitRate, int index, int quality) {
        if (videoBitRate == null) {
            return this;
        }

        videoBitRate = BitrateUtils.toKBytes(videoBitRate);
        videoBitRate = videoBitRate * quality / 100;

        bv(index, videoBitRate + "k");
        options.add("-maxrate");
        options.add(videoBitRate + "k");
        options.add("-minrate");
        options.add(videoBitRate + "k");
        options.add("-bufsize");
        options.add(videoBitRate + "k");

        return this;
    }

    public FFmpegCommand complexFilters() {
        options.addAll(getComplexFilterOptions());

        return this;
    }

    public String[] toCmd() {
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
}

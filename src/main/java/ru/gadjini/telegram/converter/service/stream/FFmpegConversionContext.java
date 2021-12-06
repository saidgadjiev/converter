package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FFmpegConversionContext {

    public static final String AUDIO_BITRATE = "ab";

    public static final String FREQUENCY = "fq";

    public static final String MAP_AUDIO_INDEX = "mai";

    public static final String VIDEO_WATERMARK = "vmark";

    public static final String GARBAGE_FILE_COLLECTOR = "gfile";

    public static final String EXTRACT_AUDIO_INDEX = "exti";

    public static final String BASS_BOOST = "bs";

    public static final String QUEUE_ITEM = "qitem";

    public static final String SQUARE_SIZE = "sqs";

    public static final String CUT_START_POINT = "ss";

    public static final String STREAM_DURATION = "t";

    public static final String SETTINGS_STATE = "stgs";

    public static final String AUDIO_STREAMS_COUNT = "acn";

    public static final String SUBTITLE_STREAMS_COUNT = "scn";

    public static final String TARGET_RESOLUTION = "trs";

    private List<FFprobeDevice.FFProbeStream> streams;

    private Format outputFormat;

    private List<SmartTempFile> inputs = new ArrayList<>();

    private SmartTempFile output;

    private boolean useStaticVideoFilter;

    private boolean useStaticAudioFilter;

    private boolean useCrf;

    private Map<String, Object> extra = new HashMap<>();

    private FFmpegConversionContext() {

    }

    public List<FFprobeDevice.FFProbeStream> streams() {
        return this.streams;
    }

    public Format outputFormat() {
        return this.outputFormat;
    }

    public SmartTempFile output() {
        return this.output;
    }

    public FFmpegConversionContext streams(final List<FFprobeDevice.FFProbeStream> streams) {
        this.streams = streams;
        return this;
    }

    public FFmpegConversionContext outputFormat(final Format outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    public FFmpegConversionContext output(final SmartTempFile tempFile) {
        this.output = tempFile;
        return this;
    }

    public List<FFprobeDevice.FFProbeStream> videoStreams() {
        return filterStreams(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE);
    }

    public List<FFprobeDevice.FFProbeStream> audioStreams() {
        return filterStreams(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE);
    }

    public List<FFprobeDevice.FFProbeStream> subtitleStreams() {
        return filterStreams(FFprobeDevice.FFProbeStream.SUBTITLE_CODEC_TYPE);
    }

    private List<FFprobeDevice.FFProbeStream> filterStreams(String codecType) {
        return streams.stream()
                .filter(f -> f.getCodecType().equals(codecType))
                .collect(Collectors.toList());
    }

    public SmartTempFile getInput() {
        return inputs.iterator().next();
    }

    public List<SmartTempFile> getInputs() {
        return inputs;
    }

    public FFmpegConversionContext input(SmartTempFile input) {
        inputs.add(input);

        return this;
    }

    public FFmpegConversionContext putExtra(String name, Object ex) {
        extra.put(name, ex);
        return this;
    }

    public <T> T getExtra(String name) {
        return (T) extra.get(name);
    }

    public boolean isUseStaticVideoFilter() {
        return useStaticVideoFilter;
    }

    public FFmpegConversionContext useStaticVideoFilter() {
        this.useStaticVideoFilter = true;
        return this;
    }

    public FFmpegConversionContext useStaticAudioFilter() {
        this.useStaticAudioFilter = true;
        return this;
    }

    public boolean isUseStaticAudioFilter() {
        return useStaticAudioFilter;
    }

    public boolean isUseCrf() {
        return useCrf;
    }

    public FFmpegConversionContext setUseCrf(boolean useCrf) {
        this.useCrf = useCrf;

        return this;
    }

    public static FFmpegConversionContext from(SmartTempFile in, SmartTempFile out,
                                               Format outputFormat, List<FFprobeDevice.FFProbeStream> streams) {
        return new FFmpegConversionContext()
                .input(in)
                .output(out)
                .outputFormat(outputFormat)
                .streams(streams);
    }

    public static FFmpegConversionContext from(SmartTempFile in, SmartTempFile out, Format outputFormat) {
        return new FFmpegConversionContext()
                .input(in)
                .output(out)
                .outputFormat(outputFormat);
    }

    public static FFmpegConversionContext from(SmartTempFile output) {
        return new FFmpegConversionContext().output(output);
    }
}

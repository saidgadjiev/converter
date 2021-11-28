package ru.gadjini.telegram.converter.service.ffmpeg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.bitrate.BitrateCalculator;
import ru.gadjini.telegram.converter.service.conversion.bitrate.BitrateCalculatorContext;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FFprobeDevice {

    private static final String STREAMS_JSON_ATTR = "streams";

    private ProcessExecutor processExecutor;

    private Jackson jsonMapper;

    private FFmpegWdhService fFmpegWdhService;

    private List<BitrateCalculator> bitrateCalculators;

    @Autowired
    public FFprobeDevice(ProcessExecutor processExecutor, Jackson jsonMapper,
                         FFmpegWdhService fFmpegWdhService,
                         List<BitrateCalculator> bitrateCalculators) {
        this.processExecutor = processExecutor;
        this.jsonMapper = jsonMapper;
        this.fFmpegWdhService = fFmpegWdhService;
        this.bitrateCalculators = bitrateCalculators;
    }

    public List<FFProbeStream> getAudioStreams(String in) throws InterruptedException {
        return getAudioStreams(in, null, true);
    }

    public List<FFProbeStream> getAudioStreams(String in, FormatCategory targetFormatCategory) throws InterruptedException {
        return getAudioStreams(in, targetFormatCategory, false);
    }

    public List<FFProbeStream> getAudioStreams(String in, FormatCategory targetFormatCategory, boolean noBitrate) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeStreamsCommand(in, FFmpegCommand.AUDIO_STREAM_SPECIFIER));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        List<FFProbeStream> streams = jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });

        if (!noBitrate) {
            setBitrateAndSizesForStreams(in, targetFormatCategory, streams);
        }

        return streams;
    }

    public List<FFProbeStream> getSubtitleStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeStreamsCommand(in, FFmpegCommand.SUBTITLES_STREAM_SPECIFIER));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        return jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });
    }

    public List<FFProbeStream> getVideoStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeStreamsCommand(in, FFmpegCommand.VIDEO_STREAM_SPECIFIER));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        return jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });
    }

    public List<FFProbeStream> getAllStreamsWithoutBitrate(String in) throws InterruptedException {
        return getAllStreams(in, null, true);
    }

    public List<FFProbeStream> getAllStreams(String in, FormatCategory targetFormatCategory) throws InterruptedException {
        return getAllStreams(in, targetFormatCategory, false);
    }

    public List<FFProbeStream> getAllStreams(String in, FormatCategory targetFormatCategory, boolean noBitrate) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeStreamsCommand(in, null));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        FFprobeResult fFprobeResult = jsonMapper.convertValue(json, FFprobeResult.class);

        for (FFProbeStream stream : fFprobeResult.getStreams()) {
            stream.setFormat(fFprobeResult.getFormat());
        }
        if (!noBitrate) {
            setBitrateAndSizesForStreams(in, targetFormatCategory, fFprobeResult.getStreams());
        }

        return fFprobeResult.getStreams();
    }

    public WHD getWHD(String in, int index) throws InterruptedException {
        return getWHD(in, index, false);
    }

    public WHD getWHD(String in, int index, boolean throwEx) throws InterruptedException {
        return fFmpegWdhService.getWHD(in, index, throwEx);
    }

    public long getDurationInSeconds(String in) throws InterruptedException {
        String duration = processExecutor.executeWithResult(getDurationCommand(in));

        return Math.round(Double.parseDouble(duration));
    }

    private String[] getDurationCommand(String in) {
        return new String[]{"ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", in};
    }

    private String[] getProbeStreamsCommand(String in, String selectStreamsTag) {
        List<String> command = new ArrayList<>();
        command.add("ffprobe");
        command.add("-v");
        command.add("error");
        if (StringUtils.isNotBlank(selectStreamsTag)) {
            command.add("-select_streams");
            command.add(selectStreamsTag);
        }
        command.add("-show_entries");
        command.add("stream=index,codec_name,codec_type,bit_rate,width,height:stream_tags=language,mimetype,filename:format=duration");
        command.add("-of");
        command.add("json");
        command.add(in);

        return command.toArray(String[]::new);
    }

    private void setBitrateAndSizesForStreams(String in, FormatCategory targetFormatCategory, List<FFProbeStream> streams) throws InterruptedException {
        setIndexes(streams);

        BitrateCalculatorContext bitrateCalculatorContext = new BitrateCalculatorContext()
                .setIn(in)
                .setTargetFormatCategory(targetFormatCategory)
                .setStreams(streams);
        for (BitrateCalculator bitrateCalculator : bitrateCalculators) {
            bitrateCalculator.prepareContext(bitrateCalculatorContext);
        }
        for (BitrateCalculator bitrateCalculator : bitrateCalculators) {
            for (FFProbeStream stream : streams) {
                if (stream.getBitRate() != null) {
                    continue;
                }
                Integer bitrate = bitrateCalculator.calculateBitrate(stream, bitrateCalculatorContext);
                stream.setBitRate(bitrate);
            }
        }
    }

    private void setIndexes(List<FFProbeStream> streams) {
        StreamIndexGenerator streamIndexGenerator = new StreamIndexGenerator();
        for (FFProbeStream stream : streams) {
            switch (stream.getCodecType()) {
                case FFProbeStream.AUDIO_CODEC_TYPE:
                    stream.setIndex(streamIndexGenerator.nextAudioStreamIndex());
                    break;
                case FFProbeStream.VIDEO_CODEC_TYPE:
                    stream.setIndex(streamIndexGenerator.nextVideoStreamIndex());
                    break;
                case FFProbeStream.SUBTITLE_CODEC_TYPE:
                    stream.setIndex(streamIndexGenerator.nextTextStreamIndex());
                    break;
            }
        }
    }

    public static class WHD {

        private Integer width;

        private Integer height;

        private Long duration;

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Long getDuration(long defaultVal) {
            return duration == null ? defaultVal : duration;
        }

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }
    }

    public static class FFprobeResult {

        private List<FFProbeStream> streams;

        private FFprobeFormat format;

        public FFprobeFormat getFormat() {
            return format;
        }

        public void setFormat(FFprobeFormat format) {
            this.format = format;
        }

        public List<FFProbeStream> getStreams() {
            return streams;
        }

        public void setStreams(List<FFProbeStream> streams) {
            this.streams = streams;
        }

        public FFProbeStream getFirstStream() {
            if (streams == null || streams.isEmpty()) {
                return null;
            }

            return streams.iterator().next();
        }

        @Override
        public String toString() {
            return "FFprobeResult{" +
                    "streams=" + streams +
                    ", format=" + format +
                    '}';
        }
    }

    public static class FFprobeFormat {

        private Double duration;

        public Long getDuration() {
            return duration == null ? null : duration.longValue();
        }

        public void setDuration(Double duration) {
            this.duration = duration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FFprobeFormat that = (FFprobeFormat) o;

            return duration != null ? duration.equals(that.duration) : that.duration == null;
        }

        @Override
        public int hashCode() {
            return duration != null ? duration.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "FFprobeFormat{" +
                    "duration=" + duration +
                    '}';
        }
    }

    public static class FFProbeStream {

        public static final String VIDEO_CODEC_TYPE = "video";

        public static final String SUBTITLE_CODEC_TYPE = "subtitle";

        public static final String AUDIO_CODEC_TYPE = "audio";

        private static final String LANGUAGE_TAG = "language";

        private static final String MIMETYPE_TAG = "mimetype";

        private static final String FILENAME_TAG = "filename";

        private int index;

        @JsonProperty("codec_name")
        private String codecName;

        private Map<String, Object> tags;

        @JsonProperty("codec_type")
        private String codecType;

        private Integer width;

        private Integer height;

        @JsonProperty("bit_rate")
        private Integer bitRate;

        private int input = 0;

        private Long streamSize;

        private FFprobeFormat format;

        @JsonIgnore
        private Integer targetBitrate;

        @JsonIgnore
        private String targetCodecName;

        @JsonIgnore
        private String targetScale;

        @JsonIgnore
        private boolean dontCopy;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public void setCodecName(String codecName) {
            this.codecName = codecName;
        }

        public Map<String, Object> getTags() {
            return tags;
        }

        public void setTags(Map<String, Object> tags) {
            this.tags = tags;
        }

        public void setCodecType(String codecType) {
            this.codecType = codecType;
        }

        public void setStreamSize(Long streamSize) {
            this.streamSize = streamSize;
        }

        public Long getStreamSize() {
            return streamSize;
        }

        public String getCodecName() {
            return codecName;
        }

        public String getLanguage() {
            return tags != null ? (String) tags.get(LANGUAGE_TAG) : null;
        }

        public String getMimeType() {
            return tags != null ? (String) tags.getOrDefault(MIMETYPE_TAG.toUpperCase(), tags.get(MIMETYPE_TAG)) : null;
        }

        public String getFileName() {
            return tags != null ? (String) tags.getOrDefault(FILENAME_TAG.toUpperCase(), tags.get(FILENAME_TAG)) : null;
        }

        public String getCodecType() {
            return codecType;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public void setInput(int input) {
            this.input = input;
        }

        public int getInput() {
            return input;
        }

        public Integer getBitRate() {
            return bitRate;
        }

        public void setBitRate(Integer bitRate) {
            this.bitRate = bitRate;
        }

        public FFprobeFormat getFormat() {
            return format;
        }

        public void setFormat(FFprobeFormat format) {
            this.format = format;
        }

        public Long getDuration() {
            return format == null ? null : format.getDuration();
        }

        public void setTargetBitrate(Integer targetBitrate) {
            this.targetBitrate = targetBitrate;
        }

        public Integer getTargetBitrate() {
            return targetBitrate;
        }

        public void setTargetCodecName(String targetCodecName) {
            this.targetCodecName = targetCodecName;
        }

        public String getTargetCodecName() {
            return targetCodecName;
        }

        public String getTargetScale() {
            return targetScale;
        }

        public void setTargetScale(String targetScale) {
            this.targetScale = targetScale;
        }

        public boolean isDontCopy() {
            return dontCopy;
        }

        public void setDontCopy(boolean dontCopy) {
            this.dontCopy = dontCopy;
        }

        @Override
        public String toString() {
            return "FFProbeStream{" +
                    "index=" + index +
                    ", codecName='" + codecName + '\'' +
                    ", tags=" + tags +
                    ", codecType='" + codecType + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    ", bitRate=" + bitRate +
                    ", input=" + input +
                    ", streamSize=" + streamSize +
                    ", format=" + format +
                    '}';
        }
    }
}


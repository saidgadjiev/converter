package ru.gadjini.telegram.converter.service.ffmpeg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.mediainfo.MediaInfoService;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FFprobeDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFprobeDevice.class);

    private static final String STREAMS_JSON_ATTR = "streams";

    private ProcessExecutor processExecutor;

    private Jackson jsonMapper;

    private MediaInfoService mediaInfoService;

    @Autowired
    public FFprobeDevice(ProcessExecutor processExecutor, Jackson jsonMapper,
                         MediaInfoService mediaInfoService) {
        this.processExecutor = processExecutor;
        this.jsonMapper = jsonMapper;
        this.mediaInfoService = mediaInfoService;
    }

    public List<FFProbeStream> getAudioStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeStreamsCommand(in, FFmpegCommand.AUDIO_STREAM_SPECIFIER));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        List<FFProbeStream> streams = jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });

        setBitrateAndSizesForStreams(in, streams);

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

        List<FFProbeStream> streams = jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });

        setBitrateAndSizesForStreams(in, streams);

        return streams;
    }

    public List<FFProbeStream> getAllStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeStreamsCommand(in));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        FFprobeResult fFprobeResult = jsonMapper.convertValue(json, FFprobeResult.class);

        for (FFProbeStream stream : fFprobeResult.getStreams()) {
            stream.setFormat(fFprobeResult.getFormat());
        }
        setBitrateAndSizesForStreams(in, fFprobeResult.getStreams());

        return fFprobeResult.getStreams();
    }

    public WHD getWHD(String in, int index) throws InterruptedException {
        return getWHD(in, index, false);
    }

    public WHD getWHD(String in, int index, boolean throwEx) throws InterruptedException {
        WHD whd = new WHD();
        try {
            FFprobeDevice.FFprobeResult probeVideoStream = probeVideoStream(in, index);
            if (probeVideoStream != null) {
                FFProbeStream videoStream = probeVideoStream.getFirstStream();
                if (videoStream != null) {
                    whd.setWidth(videoStream.getWidth());
                    whd.setHeight(videoStream.getHeight());
                }
                FFprobeDevice.FFprobeFormat fFprobeFormat = probeVideoStream.getFormat();
                if (fFprobeFormat != null) {
                    Long duration = probeVideoStream.getFormat().getDuration();
                    whd.setDuration(duration);
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            if (throwEx) {
                throw e;
            } else {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return whd;
    }

    public long getDurationInSeconds(String in) throws InterruptedException {
        String duration = processExecutor.executeWithResult(getDurationCommand(in));

        return Math.round(Double.parseDouble(duration));
    }

    private String[] getDurationCommand(String in) {
        return new String[]{"ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", in};
    }

    private FFprobeResult probeVideoStream(String in, int index) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeStreamsCommand(in, FFmpegCommand.VIDEO_STREAM_SPECIFIER, index));
        return jsonMapper.readValue(result, FFprobeResult.class);
    }

    private String[] getProbeStreamsCommand(String in) {
        return getProbeStreamsCommand(in, null);
    }

    private String[] getProbeStreamsCommand(String in, String selectStreamsTag) {
        return getProbeStreamsCommand(in, selectStreamsTag, null);
    }

    private String[] getProbeStreamsCommand(String in, String selectStreamsTag, Integer streamIndex) {
        List<String> command = new ArrayList<>();
        command.add("ffprobe");
        command.add("-v");
        command.add("error");
        if (StringUtils.isNotBlank(selectStreamsTag)) {
            command.add("-select_streams");
            if (streamIndex == null) {
                command.add(selectStreamsTag);
            } else {
                command.add(selectStreamsTag + ":" + streamIndex);
            }
        }
        command.add("-show_entries");
        command.add("stream=index,codec_name,codec_type,width,height,bit_rate:stream_tags=language,mimetype,filename:format=duration");
        command.add("-of");
        command.add("json");
        command.add(in);

        return command.toArray(String[]::new);
    }

    private void setBitrateAndSizesForStreams(String in, List<FFProbeStream> streams) throws InterruptedException {
        List<MediaInfoService.MediaInfoTrack> tracks = mediaInfoService.getTracks(in);

        streams.forEach(ffProbeStream -> tracks.stream().filter(t -> Objects.equals(ffProbeStream.index, t.getStreamOrder()) &&
                Objects.equals(t.getType(), getMediaInfoStreamTypeByFFmpegCodecType(ffProbeStream.getCodecType())))
                .findFirst()
                .ifPresent(mediaInfoTrack -> {
                    if (mediaInfoTrack.getBitRate() != null) {
                        ffProbeStream.setBitRate(mediaInfoTrack.getBitRate());
                    }
                    ffProbeStream.streamSize = mediaInfoTrack.getStreamSize();
                    if (FFProbeStream.VIDEO_CODEC_TYPE.equals(ffProbeStream.getCodecType())
                            && ffProbeStream.getBitRate() == null
                            && ffProbeStream.getStreamSize() != null
                    ) {
                        ffProbeStream.setBitRate(calculateBitRate(ffProbeStream.getStreamSize(), ffProbeStream.getDuration()));
                    }
                }));
    }

    private int calculateBitRate(long fileSize, long duration) {
        return (int) ((fileSize / 1024 * 8) / duration);
    }

    private String getMediaInfoStreamTypeByFFmpegCodecType(String ffmpegCodecType) {
        switch (ffmpegCodecType) {
            case FFProbeStream.AUDIO_CODEC_TYPE:
                return MediaInfoService.AUDIO_TYPE;
            case FFProbeStream.VIDEO_CODEC_TYPE:
                return MediaInfoService.VIDEO_TYPE;
            case FFProbeStream.SUBTITLE_CODEC_TYPE:
                return MediaInfoService.SUBTITLE_TYPE;
            default:
                return "unknown";
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FFProbeStream stream = (FFProbeStream) o;

            if (index != stream.index) return false;
            if (input != stream.input) return false;
            if (codecName != null ? !codecName.equals(stream.codecName) : stream.codecName != null) return false;
            if (tags != null ? !tags.equals(stream.tags) : stream.tags != null) return false;
            if (codecType != null ? !codecType.equals(stream.codecType) : stream.codecType != null) return false;
            if (width != null ? !width.equals(stream.width) : stream.width != null) return false;
            if (height != null ? !height.equals(stream.height) : stream.height != null) return false;
            if (bitRate != null ? !bitRate.equals(stream.bitRate) : stream.bitRate != null) return false;
            return format != null ? format.equals(stream.format) : stream.format == null;
        }

        @Override
        public int hashCode() {
            int result = index;
            result = 31 * result + (codecName != null ? codecName.hashCode() : 0);
            result = 31 * result + (tags != null ? tags.hashCode() : 0);
            result = 31 * result + (codecType != null ? codecType.hashCode() : 0);
            result = 31 * result + (width != null ? width.hashCode() : 0);
            result = 31 * result + (height != null ? height.hashCode() : 0);
            result = 31 * result + (bitRate != null ? bitRate.hashCode() : 0);
            result = 31 * result + input;
            result = 31 * result + (format != null ? format.hashCode() : 0);
            return result;
        }
    }
}


package ru.gadjini.telegram.converter.service.ffmpeg;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.List;
import java.util.Map;

@Service
public class FFprobeDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFprobeDevice.class);

    private static final String STREAMS_JSON_ATTR = "streams";

    private ProcessExecutor processExecutor;

    private Jackson jsonMapper;

    @Autowired
    public FFprobeDevice(ProcessExecutor processExecutor, Jackson jsonMapper) {
        this.processExecutor = processExecutor;
        this.jsonMapper = jsonMapper;
    }

    public List<Stream> getAudioStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getAudioStreamsCommand(in));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        return jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });
    }

    public List<Stream> getSubtitleStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getSubtitleStreamsCommand(in));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        return jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });
    }

    public List<Stream> getVideoStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getVideoStreamsCommand(in));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        return jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });
    }

    public List<Stream> getAllStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getAllStreamsCommand(in));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        FFprobeResult fFprobeResult = jsonMapper.convertValue(json, FFprobeResult.class);

        for (Stream stream : fFprobeResult.getStreams()) {
            stream.setFormat(fFprobeResult.getFormat());
        }

        return fFprobeResult.getStreams();
    }

    public FFprobeResult probeVideoStream(String in, int index) throws InterruptedException {
        String result = processExecutor.executeWithResult(getVideoStreamCommand(in, index));
        return jsonMapper.readValue(result, FFprobeResult.class);
    }

    public WHD getWHD(String in, int index) throws InterruptedException {
        return getWHD(in, index, false);
    }

    public WHD getWHD(String in, int index, boolean throwEx) throws InterruptedException {
        WHD whd = new WHD();
        try {
            FFprobeDevice.FFprobeResult probeVideoStream = probeVideoStream(in, index);
            if (probeVideoStream != null) {
                FFprobeDevice.Stream videoStream = probeVideoStream.getFirstStream();
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

    private String[] getVideoStreamCommand(String in, int index) {
        return new String[]{"ffprobe", "-v", "error", "-select_streams", "v:" + index, "-show_entries", "stream=width,height:format=duration", "-of", "json", in};
    }

    private String[] getAudioStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "a", "-show_entries", "stream=index,codec_name,codec_type:stream_tags=language", "-of", "json", in
        };
    }

    private String[] getSubtitleStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "s", "-show_entries", "stream=index,codec_name,codec_type:stream_tags=language", "-of", "json", in
        };
    }

    private String[] getVideoStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "v", "-show_entries", "stream=index,codec_name,codec_type,width,height", "-of", "json", in
        };
    }

    private String[] getAllStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-show_entries", "stream=index,codec_name,codec_type,width,height,bit_rate:stream_tags=language:format=duration", "-of", "json", in
        };
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

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }
    }

    public static class FFprobeResult {

        private List<Stream> streams;

        private FFprobeFormat format;

        public FFprobeFormat getFormat() {
            return format;
        }

        public void setFormat(FFprobeFormat format) {
            this.format = format;
        }

        public List<Stream> getStreams() {
            return streams;
        }

        public void setStreams(List<Stream> streams) {
            this.streams = streams;
        }

        public Stream getFirstStream() {
            if (streams == null || streams.isEmpty()) {
                return null;
            }

            return streams.iterator().next();
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
    }

    public static class Stream {

        public static final String VIDEO_CODEC_TYPE = "video";

        public static final String SUBTITLE_CODEC_TYPE = "subtitle";

        public static final String AUDIO_CODEC_TYPE = "audio";

        private static final String LANGUAGE_TAG = "language";

        private int index;

        @JsonProperty("codec_name")
        private String codecName;

        private Map<String, Object> tags;

        @JsonProperty("codec_type")
        private String codecType;

        private Integer width;

        private Integer height;

        @JsonProperty("bit_rate")
        private Long bitRate;

        private int input = 0;

        private FFprobeFormat format;

        public String getCodecName() {
            return codecName;
        }

        public int getIndex() {
            return index;
        }

        public String getLanguage() {
            return tags != null ? (String) tags.get(LANGUAGE_TAG) : null;
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

        public Long getBitRate() {
            return bitRate;
        }

        public void setBitRate(Long bitRate) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Stream stream = (Stream) o;

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


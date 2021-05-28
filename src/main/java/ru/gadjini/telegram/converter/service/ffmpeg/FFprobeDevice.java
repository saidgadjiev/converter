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

    public List<Stream> getVideoStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getVideoStreamsCommand(in));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        return jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });
    }

    public List<Stream> getAllStreams(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getAllStreamsCommand(in));
        JsonNode json = jsonMapper.readValue(result, JsonNode.class);

        return jsonMapper.convertValue(json.get(STREAMS_JSON_ATTR), new TypeReference<>() {
        });
    }

    public FFprobeResult probeVideoStream(String in, int index) throws InterruptedException {
        String result = processExecutor.executeWithResult(getVideoStreamCommand(in, index));
        return jsonMapper.readValue(result, FFprobeResult.class);
    }

    public WHD getWHD(String in, int index) throws InterruptedException {
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
                    Long duration = probeVideoStream.getFormat().getDuration() != null ? probeVideoStream.getFormat().getDuration().longValue() : null;
                    whd.setDuration(duration);
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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

    private String[] getVideoStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "v", "-show_entries", "stream=index,codec_name,codec_type", "-of", "json", in
        };
    }

    private String[] getAllStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-show_entries", "stream=index,codec_name,codec_type", "-of", "json", in
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

        public Double getDuration() {
            return duration;
        }

        public void setDuration(Double duration) {
            this.duration = duration;
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

        private Integer input;

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

        public void setInput(Integer input) {
            this.input = input;
        }

        public Integer getInput() {
            return input;
        }
    }
}


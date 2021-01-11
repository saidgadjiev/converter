package ru.gadjini.telegram.converter.service.ffmpeg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.List;
import java.util.Map;

@Service
public class FFprobeDevice {

    private static final String STREAMS_JSON_ATTR = "streams";

    private ProcessExecutor processExecutor;

    private Gson gson;

    @Autowired
    public FFprobeDevice(ProcessExecutor processExecutor, Gson gson) {
        this.processExecutor = processExecutor;
        this.gson = gson;
    }

    public List<Stream> getAudioStreams(String in) {
        String result = processExecutor.executeWithResult(getAudioStreamsCommand(in));
        JsonObject json = gson.fromJson(result, JsonObject.class);

        return gson.fromJson(json.getAsJsonArray(STREAMS_JSON_ATTR), new TypeToken<List<Stream>>() {
        }.getType());
    }

    public List<Stream> getAllStreams(String in) {
        String result = processExecutor.executeWithResult(getAllStreamsCommand(in));
        JsonObject json = gson.fromJson(result, JsonObject.class);

        return gson.fromJson(json.getAsJsonArray(STREAMS_JSON_ATTR), new TypeToken<List<Stream>>() {
        }.getType());
    }

    public String getVideoCodec(String in) {
        return processExecutor.executeWithResult(getCodecNameCommand(in)).replace("\n", "");
    }

    public long getDurationInSeconds(String in) {
        String duration = processExecutor.executeWithResult(getDurationCommand(in));

        return Math.round(Double.parseDouble(duration));
    }

    private String[] getDurationCommand(String in) {
        return new String[]{"ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", in};
    }

    private String[] getAudioStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "a", "-show_entries", "stream=index:stream_tags=language", "-of", "json", in
        };
    }

    private String[] getAllStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-show_entries", "stream=index,codec_name,codec_type", "-of", "json", in
        };
    }

    private String[] getCodecNameCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=codec_name", "-of", "default=noprint_wrappers=1:nokey=1", in
        };
    }

    public static class Stream {

        public static final String VIDEO_CODEC_TYPE = "video";

        public static final String SUBTITLE_CODEC_TYPE = "subtitle";

        private static final String LANGUAGE_TAG = "language";

        private int index;

        @SerializedName("codec_name")
        private String codecName;

        private Map<String, Object> tags;

        @SerializedName("codec_type")
        private String codecType;

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
    }
}


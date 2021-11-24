package ru.gadjini.telegram.converter.service.mediainfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.ffmpeg.StreamIndexGenerator;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.List;
import java.util.Objects;

@Component
public class MediaInfoService {

    public static final String VIDEO_TYPE = "Video";

    public static final String GENERAL_TYPE = "General";

    public static final String AUDIO_TYPE = "Audio";

    public static final String SUBTITLE_TYPE = "Text";

    private ProcessExecutor processExecutor;

    private Jackson jackson;

    @Autowired
    public MediaInfoService(ProcessExecutor processExecutor, Jackson jackson) {
        this.processExecutor = processExecutor;
        this.jackson = jackson;
    }

    public List<MediaInfoTrack> getTracks(String in) throws InterruptedException {
        String result = processExecutor.executeWithResult(getMediaInfoCommand(in));
        JsonNode json = jackson.readValue(result, JsonNode.class);

        MediaInfoResult mediaInfoResult = jackson.convertValue(json, MediaInfoResult.class);

        if (mediaInfoResult.media == null) {
            return null;
        }

        List<MediaInfoTrack> tracks = Objects.requireNonNullElseGet(mediaInfoResult.media.track, List::of);
        StreamIndexGenerator streamIndexGenerator = new StreamIndexGenerator();
        for (MediaInfoTrack mediaInfoTrack : tracks) {
            switch (mediaInfoTrack.getType()) {
                case AUDIO_TYPE:
                    mediaInfoTrack.index = streamIndexGenerator.nextAudioStreamIndex();
                    break;
                case VIDEO_TYPE:
                    mediaInfoTrack.index = streamIndexGenerator.nextVideoStreamIndex();
                    break;
                case SUBTITLE_TYPE:
                    mediaInfoTrack.index = streamIndexGenerator.nextTextStreamIndex();
                    break;
            }
        }

        return tracks;
    }

    private String[] getMediaInfoCommand(String in) {
        return new String[]{
                "mediainfo", in, "--Output=JSON"
        };
    }

    public static class MediaInfoTrack {

        @JsonProperty("@type")
        private String type;

        @JsonIgnore
        private int index;

        @JsonProperty("BitRate")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Integer bitRate;

        @JsonProperty("StreamSize")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Long streamSize;

        @JsonProperty("OverallBitRate")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Integer overallBitRate;

        public String getType() {
            return type;
        }

        public Integer getIndex() {
            return index;
        }

        public Integer getBitRate() {
            return bitRate;
        }

        public Long getStreamSize() {
            return streamSize;
        }

        public Integer getOverallBitRate() {
            return overallBitRate;
        }
    }

    private static class MediaInfoResult {

        private MediaInfoMedia media;

        public MediaInfoMedia getMedia() {
            return media;
        }

        public void setMedia(MediaInfoMedia media) {
            this.media = media;
        }
    }

    private static class MediaInfoMedia {

        private List<MediaInfoTrack> track;

        public List<MediaInfoTrack> getTrack() {
            return track;
        }

        public void setTrack(List<MediaInfoTrack> track) {
            this.track = track;
        }
    }
}

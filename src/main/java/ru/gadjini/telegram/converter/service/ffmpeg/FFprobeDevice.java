package ru.gadjini.telegram.converter.service.ffmpeg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FFprobeDevice {

    private static final Pattern STREAM_INDEXES_PATTERN = Pattern.compile("index=(?<index>\\d+)");

    private static final Pattern CODEC_NAME_PATTERN = Pattern.compile("codec_name=(?<codec>[a-z1-9]+)");

    private static final Pattern CODEC_TYPE_PATTERN = Pattern.compile("codec_type=(?<codectype>(audio|video|subtitle|data|attachments))");

    private static final Pattern STREAM_LANGUAGE_PATTERN = Pattern.compile("TAG:language=(?<language>[a-z]+)");

    private ProcessExecutor processExecutor;

    @Autowired
    public FFprobeDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public List<Stream> getAudioStreams(String in) {
        List<Stream> streams = new ArrayList<>();
        String result = processExecutor.executeWithResult(getAudioStreamsCommand(in));
        String[] streamInfos = result.split("\\[/STREAM]");

        for (String streamInfo : streamInfos) {
            Matcher indexesMatcher = STREAM_INDEXES_PATTERN.matcher(streamInfo);
            Matcher languageMatcher = STREAM_LANGUAGE_PATTERN.matcher(streamInfo);

            if (indexesMatcher.find()) {
                int index = Integer.parseInt(indexesMatcher.group("index"));
                String language = null;
                if (languageMatcher.find()) {
                    language = languageMatcher.group("language");
                }

                streams.add(new Stream(index, language));
            }
        }

        return streams;
    }

    public List<Stream> getAllStreams(String in) {
        List<Stream> streams = new ArrayList<>();
        String result = processExecutor.executeWithResult(getAllStreamsCommand(in));
        String[] streamInfos = result.split("\\[/STREAM]");

        for (String streamInfo : streamInfos) {
            Matcher indexesMatcher = STREAM_INDEXES_PATTERN.matcher(streamInfo);

            if (indexesMatcher.find()) {
                int index = Integer.parseInt(indexesMatcher.group("index"));
                String codec = null;
                Matcher codecMatcher = CODEC_NAME_PATTERN.matcher(streamInfo);
                if (codecMatcher.find()) {
                    codec = codecMatcher.group("codec");
                }
                String codecType = null;
                Matcher codecTypeMatcher = CODEC_TYPE_PATTERN.matcher(streamInfo);
                if (codecTypeMatcher.find()) {
                    codecType = codecTypeMatcher.group("codectype");
                }

                streams.add(new Stream(index, codec, codecType));
            }
        }

        return streams;
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
                "ffprobe", "-v", "error", "-select_streams", "a", "-show_entries", "stream=index:stream_tags=language", in
        };
    }

    private String[] getAllStreamsCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-show_entries", "stream=index,codec_name,codec_type", in
        };
    }

    private String[] getCodecNameCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=codec_name", "-of", "default=noprint_wrappers=1:nokey=1", in
        };
    }

    public static class Stream {

        public static final String AUDIO_CODEC_TYPE = "audio";

        public static final String VIDEO_CODEC_TYPE = "video";

        public static final String SUBTITLE_CODEC_TYPE = "subtitle";

        public static final String DATA_CODEC_TYPE = "data";

        private int index;

        private String codec;

        private String language;

        private String codecType;

        private Stream(int index, String codec, String language, String codecType) {
            this.index = index;
            this.codec = codec;
            this.language = language;
            this.codecType = codecType;
        }


        private Stream(int index, String language) {
            this(index, null, language, null);
        }

        private Stream(int index, String codec, String codecType) {
            this(index, codec, null, codecType);
        }

        public String getCodec() {
            return codec;
        }

        public int getIndex() {
            return index;
        }

        public String getLanguage() {
            return language;
        }

        public String getCodecType() {
            return codecType;
        }
    }
}


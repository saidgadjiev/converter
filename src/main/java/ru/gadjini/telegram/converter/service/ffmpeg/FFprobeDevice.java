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

    private static final Pattern STREAM_LANGUAGE_PATTERN = Pattern.compile("TAG:language=(?<language>[a-z]+)");

    private ProcessExecutor processExecutor;

    @Autowired
    public FFprobeDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public List<AudioStream> getAudioStreams(String in) {
        List<AudioStream> streams = new ArrayList<>();
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

                streams.add(new AudioStream(index, language));
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

    private String[] getCodecNameCommand(String in) {
        return new String[]{
                "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=codec_name", "-of", "default=noprint_wrappers=1:nokey=1", in
        };
    }

    public static void main(String[] args) {
        String str = "[STREAM]                   \n" +
                "index=1                    \n" +
                "TAG:language=           \n";

        String arr[] = str.split("/STREAM");
        Pattern pattern = Pattern.compile("index=(?<index>\\d+)");
        Pattern languagePattern = Pattern.compile("TAG:language=(?<language>[a-z]+)");
        for (String s : arr) {
            Matcher matcher = pattern.matcher(s);
            Matcher languageMatcher = languagePattern.matcher(s);

            if (matcher.find()) {
                System.out.println(matcher.group("index"));
            }

            if (languageMatcher.find()) {
                System.out.println(languageMatcher.group("language"));
            }
        }
    }

    public static class AudioStream {

        private int index;

        private String language;

        public AudioStream(int index, String language) {
            this.index = index;
            this.language = language;
        }

        public int getIndex() {
            return index;
        }

        public String getLanguage() {
            return language;
        }
    }
}


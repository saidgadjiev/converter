package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FFmpegVideoStreamDetector {

    private FFmpegImageStreamDetector imageStreamDetector;

    @Autowired
    public FFmpegVideoStreamDetector(FFmpegImageStreamDetector imageStreamDetector) {
        this.imageStreamDetector = imageStreamDetector;
    }

    public boolean isExtraVideoStream(List<FFprobeDevice.FFProbeStream> videoStreams, FFprobeDevice.FFProbeStream videoStream) {
        if (videoStreams.size() == 1 && imageStreamDetector.isImageStream(videoStream)) {
            return false;
        }
        if (videoStreams.stream().allMatch(f -> imageStreamDetector.isImageStream(f))) {
            return false;
        }

        return imageStreamDetector.isImageStream(videoStream);
    }

    public int getFirstVideoStreamIndex(List<FFprobeDevice.FFProbeStream> allStreams) {
        List<FFprobeDevice.FFProbeStream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.FFProbeStream videoStream = videoStreams.get(videoStreamMapIndex);
            if (!isExtraVideoStream(videoStreams, videoStream)) {
                return videoStreamMapIndex;
            }
        }

        return 0;
    }
}

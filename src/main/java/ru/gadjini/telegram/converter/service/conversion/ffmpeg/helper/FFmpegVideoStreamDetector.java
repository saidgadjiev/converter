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

    public int getFirstVideoStreamIndex(List<FFprobeDevice.FFProbeStream> VideoStreams) {
        List<FFprobeDevice.FFProbeStream> videoStreams = VideoStreams.stream()
                .filter(s -> FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        return getFirstVideoStreamIndex0(videoStreams);
    }

    public FFprobeDevice.FFProbeStream getFirstVideoStream(List<FFprobeDevice.FFProbeStream> allStreams) {
        List<FFprobeDevice.FFProbeStream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        int firstVideoStream = getFirstVideoStreamIndex0(videoStreams);


        return videoStreams.get(firstVideoStream);
    }

    private int getFirstVideoStreamIndex0(List<FFprobeDevice.FFProbeStream> videoStreams) {
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.FFProbeStream videoStream = videoStreams.get(videoStreamMapIndex);
            if (!isExtraVideoStream(videoStreams, videoStream)) {
                return videoStreamMapIndex;
            }
        }

        return 0;
    }
}

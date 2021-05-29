package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FFmpegVideoConversionHelper {

    private static final Set<String> IMAGE_CODECS = Set.of(FFmpegCommandBuilder.BMP, FFmpegCommandBuilder.PNG, FFmpegCommandBuilder.MJPEG);

    public static boolean isExtraVideoStream(List<FFprobeDevice.Stream> videoStreams, FFprobeDevice.Stream videoStream) {
        if (videoStreams.size() == 1 && isImageStream(videoStream.getCodecName())) {
            return false;
        }
        if (videoStreams.stream().allMatch(s -> isImageStream(s.getCodecName()))) {
            return false;
        }

        return isImageStream(videoStream.getCodecName());
    }

    public static int getFirstVideoStreamIndex(List<FFprobeDevice.Stream> allStreams) {
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamMapIndex);
            if (FFmpegVideoConversionHelper.isExtraVideoStream(videoStreams, videoStream)) {
                continue;
            }

            return videoStreamMapIndex;
        }

        return 0;
    }

    private static boolean isImageStream(String codecName) {
        return IMAGE_CODECS.contains(codecName);
    }
}

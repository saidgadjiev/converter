package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import java.util.List;

public class FFmpegVideoConversionHelper {

    public static boolean isExtraVideoStream(List<FFprobeDevice.Stream> videoStreams, FFprobeDevice.Stream videoStream) {
        if (videoStreams.size() == 1 && isImageStream(videoStream.getCodecName())) {
            return false;
        }
        if (videoStreams.stream().allMatch(s -> isImageStream(s.getCodecName()))) {
            return false;
        }

        return isImageStream(videoStream.getCodecName());
    }

    private static boolean isImageStream(String codecName) {
        return FFmpegCommandBuilder.MJPEG.equals(codecName) || FFmpegCommandBuilder.BMP.equals(codecName);
    }
}

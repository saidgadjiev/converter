package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;

@Service
public class FFmpegVideoConversionHelper {

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegVideoConversionHelper(FFprobeDevice fFprobeDevice) {
        this.fFprobeDevice = fFprobeDevice;
    }

    public List<FFprobeDevice.Stream> getStreamsForConversion(SmartTempFile file) throws InterruptedException {
        List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
        FFmpegVideoConversionHelper.removeExtraVideoStreams(allStreams);

        return allStreams;
    }

    private static void removeExtraVideoStreams(List<FFprobeDevice.Stream> streams) {
        FFprobeDevice.Stream videoStreamToCopy = streams.stream()
                .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())
                        && isNotImageStream(s.getCodecName()))
                .findFirst().orElse(null);

        if (videoStreamToCopy == null) {
            return;
        }

        streams.removeIf(stream -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(stream.getCodecType())
                && videoStreamToCopy.getIndex() != stream.getIndex());
    }

    private static boolean isNotImageStream(String codecName) {
        return !"mjpeg".equals(codecName) && !"bmp".equals(codecName);
    }
}

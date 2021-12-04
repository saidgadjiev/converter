package ru.gadjini.telegram.converter.service.conversion.bitrate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.mediainfo.MediaInfoService;

import java.util.List;
import java.util.Objects;

@Component
@Order(1)
public class MediaInfoBitrateCalculator implements BitrateCalculator {

    private MediaInfoService mediaInfoService;

    @Autowired
    public MediaInfoBitrateCalculator(MediaInfoService mediaInfoService) {
        this.mediaInfoService = mediaInfoService;
    }

    @Override
    public void prepareContext(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException {
        List<MediaInfoService.MediaInfoTrack> tracks = mediaInfoService.getTracks(bitrateCalculatorContext.getIn());
        bitrateCalculatorContext.setMediaInfoTracks(tracks);
    }

    @Override
    public Integer calculateBitrate(FFprobeDevice.FFProbeStream ffProbeStream, BitrateCalculatorContext bitrateCalculatorContext) {
        return bitrateCalculatorContext.getMediaInfoTracks().stream()
                .filter(t -> Objects.equals(ffProbeStream.getIndex(), t.getIndex()) &&
                        Objects.equals(t.getType(), getMediaInfoStreamTypeByFFmpegCodecType(ffProbeStream.getCodecType())))
                .findFirst()
                .orElse(new MediaInfoService.MediaInfoTrack())
                .getBitRate();
    }

    private String getMediaInfoStreamTypeByFFmpegCodecType(String ffmpegCodecType) {
        switch (ffmpegCodecType) {
            case FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE:
                return MediaInfoService.AUDIO_TYPE;
            case FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE:
                return MediaInfoService.VIDEO_TYPE;
            case FFprobeDevice.FFProbeStream.SUBTITLE_CODEC_TYPE:
                return MediaInfoService.SUBTITLE_TYPE;
            default:
                return "unknown";
        }
    }
}

package ru.gadjini.telegram.converter.service.conversion.bitrate.overall;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.bitrate.BitrateCalculatorContext;
import ru.gadjini.telegram.converter.service.mediainfo.MediaInfoService;

import java.util.List;

@Component
@Order(1)
public class MediaInfoOverallBitrateCalculator implements VideoOverallBitrateCalculator {

    private MediaInfoService mediaInfoService;

    @Autowired
    public MediaInfoOverallBitrateCalculator(MediaInfoService mediaInfoService) {
        this.mediaInfoService = mediaInfoService;
    }

    @Override
    public Integer calculate(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException {
        List<MediaInfoService.MediaInfoTrack> mediaInfoTracks = bitrateCalculatorContext.getMediaInfoTracks();
        if (mediaInfoTracks == null) {
            mediaInfoTracks = mediaInfoService.getTracks(bitrateCalculatorContext.getIn());
        }

        return mediaInfoTracks.stream()
                .filter(t -> MediaInfoService.GENERAL_TYPE.equals(t.getType()))
                .findFirst()
                .orElse(new MediaInfoService.MediaInfoTrack())
                .getOverallBitRate();
    }
}

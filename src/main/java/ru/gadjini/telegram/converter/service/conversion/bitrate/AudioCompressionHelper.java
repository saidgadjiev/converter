package ru.gadjini.telegram.converter.service.conversion.bitrate;

import ru.gadjini.telegram.converter.service.conversion.bitrate.searcher.AudioBitrateByResolutionSearcher;

import java.util.List;
import java.util.Objects;

public class AudioCompressionHelper {

    private static final List<Integer> RESOLUTIONS = List.of(1080, 720, 480, 360, 240, 144);

    private AudioCompressionHelper() { }

    public static Integer getAudioBitrateForCompression(int resolution, List<Integer> currentAudioBitrate) {
        int overallAudioBitrate = currentAudioBitrate.stream()
                .filter(Objects::nonNull).reduce(0, Integer::sum);
        if (overallAudioBitrate == 0) {
            return null;
        }
        int currentAverageAudioBitrate = overallAudioBitrate / currentAudioBitrate.size() - 2000;
        int audioBitrateForCompression = AudioBitrateByResolutionSearcher.getAudioBitrate(resolution);

        if (audioBitrateForCompression >= currentAverageAudioBitrate) {
            for (Integer res : RESOLUTIONS) {
                if (res < resolution) {
                    audioBitrateForCompression = AudioBitrateByResolutionSearcher.getAudioBitrate(res);

                    if (audioBitrateForCompression < currentAverageAudioBitrate) {
                        break;
                    }
                }
            }
        }

        return audioBitrateForCompression;
    }
}

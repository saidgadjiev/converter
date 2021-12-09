package ru.gadjini.telegram.converter.service.conversion.bitrate.searcher;

import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.utils.BitrateUtils;

import java.util.List;
import java.util.Map;

public class AudioBitrateByResolutionSearcher {

    private static final Map<Integer, Integer> AUDIO_BITRATE_BY_RESOLUTION = Map.of(
            1080, BitrateUtils.toBytes(128),
            720, BitrateUtils.toBytes(128),
            480, BitrateUtils.toBytes(96),
            360, BitrateUtils.toBytes(96),
            240, BitrateUtils.toBytes(64),
            144, BitrateUtils.toBytes(32)
    );

    private AudioBitrateByResolutionSearcher() {

    }

    public static int getAudioBitrate(int resolution) {
        if (AUDIO_BITRATE_BY_RESOLUTION.containsKey(resolution)) {
            return AUDIO_BITRATE_BY_RESOLUTION.get(resolution);
        }
        List<Integer> resolutions = EditVideoResolutionState.AVAILABLE_RESOLUTIONS;
        int distance = Math.abs(resolutions.get(0) - resolution);
        int idx = 0;
        for (int c = 1; c < resolutions.size(); c++) {
            int cdistance = Math.abs(resolutions.get(c) - resolution);
            if (cdistance < distance) {
                idx = c;
                distance = cdistance;
            }
        }

        return AUDIO_BITRATE_BY_RESOLUTION.get(resolutions.get(idx));
    }
}

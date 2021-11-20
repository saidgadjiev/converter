package ru.gadjini.telegram.converter.service.conversion.bitrate.searcher;

import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AudioBitrateByResolutionSearcher {

    public static final Map<Integer, Integer> AUDIO_BITRATE_BY_RESOLUTION = Map.of(
            1080, 128 * 1024,
            720, 64 * 1024,
            480, 64 * 1024,
            360, 64 * 1024,
            240, 32 * 1024,
            144, 32 * 1024
    );

    private AudioBitrateByResolutionSearcher() {

    }

    public static int getAudioBitrate(int resolution) {
        if (AUDIO_BITRATE_BY_RESOLUTION.containsKey(resolution)) {
            return AUDIO_BITRATE_BY_RESOLUTION.get(resolution);
        }
        List<Integer> resolutions = new ArrayList<>(EditVideoResolutionState.VIDEO_BITRATE_BY_RESOLUTION.keySet());
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

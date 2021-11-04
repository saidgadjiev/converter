package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VideoAudioBitrateCalculator {

    private VideoAudioBitrateCalculator() {

    }

    public static void calculateVideoAudioBitrate(int currentOverallBitrate, int currentVideoBitrate,
                                                  int targetOverallBitrate, int targetAudioBitrate,
                                                  List<Integer> currentAudioBitrate, AtomicInteger videoBitrate,
                                                  AtomicInteger audioBitrate,
                                                  Collection<Integer> audioBitrateVariations) {
        int maxCurrentAudioBitrate = currentAudioBitrate.stream().max(Integer::compareTo).get();
        if (maxCurrentAudioBitrate < targetAudioBitrate) {
            targetAudioBitrate = maxCurrentAudioBitrate;
        }

        calculateVideoAudioBitrate(currentOverallBitrate, currentVideoBitrate, targetOverallBitrate,
                targetAudioBitrate, currentAudioBitrate, videoBitrate, audioBitrate);
        if (videoBitrate.get() < 0) {
            for (Integer lessAudioBitrateVariation : audioBitrateVariations.stream()
                    .sorted((f1, f2) -> Integer.compare(f2, f1)).distinct().collect(Collectors.toList())) {
                if (lessAudioBitrateVariation < targetAudioBitrate) {
                    calculateVideoAudioBitrate(currentOverallBitrate, currentVideoBitrate, targetOverallBitrate,
                            lessAudioBitrateVariation, currentAudioBitrate, videoBitrate, audioBitrate);
                    if (videoBitrate.get() > 0) {
                        return;
                    }
                }
            }
        }
    }

    private static void calculateVideoAudioBitrate(int currentOverallBitrate, int currentVideoBitrate,
                                                  int targetOverallBitrate, int targetAudioBitrate,
                                                  List<Integer> currentAudioBitrate, AtomicInteger videoBitrate,
                                                  AtomicInteger audioBitrate) {
        int diff = currentOverallBitrate - targetOverallBitrate;

        for (Integer bitrate : currentAudioBitrate) {
            if (bitrate < targetAudioBitrate) {
                diff += targetAudioBitrate - bitrate;
            } else {
                diff -= bitrate - targetAudioBitrate;
            }
        }
        audioBitrate.set(targetAudioBitrate);
        if (diff <= 0) {
            videoBitrate.set(currentVideoBitrate);
        } else {
            videoBitrate.set(currentVideoBitrate - diff);
        }
    }
}

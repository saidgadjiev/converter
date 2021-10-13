package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoAudioBitrateCalculator {

    private VideoAudioBitrateCalculator() {

    }

    public static void calculateVideoAudioBitrate(int currentOverallBitrate, int currentVideoBitrate,
                                           int targetOverallBitrate, int targetAudioBitrate,
                                           List<Integer> currentAudioBitrate, AtomicInteger videoBitrate,
                                           AtomicInteger audioBitrate) {
        int diff = currentOverallBitrate - targetOverallBitrate;

        for (Integer bitrate : currentAudioBitrate) {
            if (bitrate < targetAudioBitrate) {
                diff -= bitrate - targetAudioBitrate;
            } else {
                diff += targetAudioBitrate - bitrate;
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

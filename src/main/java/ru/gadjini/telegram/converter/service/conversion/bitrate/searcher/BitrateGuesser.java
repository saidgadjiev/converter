package ru.gadjini.telegram.converter.service.conversion.bitrate.searcher;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BitrateGuesser {

    private static final int MIN_VIDEO_BITRATE = 128 * 1024;

    private static final int IMAGE_VIDEO_STREAM_BITRATE = 20;

    private static final List<Integer> AVAILABLE_AUDIO_BITRATES = List.of(128, 96, 64, 32, 16, 8);

    public static void guessAudioFileBitrate(int overallBitrate,
                                             int audioStreamsCount, AtomicInteger audioBitrateResult) {
        if (audioStreamsCount > 0) {
            audioBitrateResult.set(overallBitrate / audioStreamsCount);
        }
    }

    public static void guessVideoFileBitrate(int overallBitrate, int startAudioBitrate,
                                    int videoStreamsCount, int audioStreamsCount, int imageVideoStreamsCount,
                                    AtomicInteger videoBitrateResult, AtomicInteger audioBitrateResult,
                                    AtomicInteger imageVideoBitrateResult) {
        if (imageVideoStreamsCount > 0) {
            imageVideoBitrateResult.set(IMAGE_VIDEO_STREAM_BITRATE);
            overallBitrate -= imageVideoStreamsCount * IMAGE_VIDEO_STREAM_BITRATE;
        }
        calculateVideoAudioBitrate(overallBitrate, startAudioBitrate, videoStreamsCount,
                audioStreamsCount, videoBitrateResult, audioBitrateResult);
        if (videoBitrateResult.get() < MIN_VIDEO_BITRATE && videoStreamsCount > 0) {
            for (Integer lessAudioBitrateVariation : AVAILABLE_AUDIO_BITRATES) {
                if (lessAudioBitrateVariation < startAudioBitrate) {
                    calculateVideoAudioBitrate(overallBitrate, lessAudioBitrateVariation, videoStreamsCount,
                            audioStreamsCount, videoBitrateResult, audioBitrateResult);
                    if (videoBitrateResult.get() >= MIN_VIDEO_BITRATE) {
                        return;
                    }
                }
            }
        }
    }

    private static void calculateVideoAudioBitrate(int overallBitrate, int audioBitrate,
                                                   int videoStreamsCount, int audioStreamsCount,
                                                   AtomicInteger videoBitrateResult, AtomicInteger audioBitrateResult) {
        if (videoStreamsCount == 0) {
            if (audioStreamsCount == 0) {
                return;
            }
            audioBitrateResult.set(overallBitrate / audioStreamsCount);
        } else {
            if (audioStreamsCount > 0) {
                overallBitrate -= audioStreamsCount * audioBitrate;
                audioBitrateResult.set(audioBitrate);
            }
            int videoBitrate = overallBitrate / videoStreamsCount;

            videoBitrateResult.set(videoBitrate);
        }
    }
}

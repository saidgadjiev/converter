package ru.gadjini.telegram.converter.service.ffmpeg;

import java.util.concurrent.atomic.AtomicInteger;

public class StreamIndexGenerator {

    private AtomicInteger audioStreamIndex = new AtomicInteger(0);
    private AtomicInteger videoStreamIndex = new AtomicInteger(0);
    private AtomicInteger textStreamIndex = new AtomicInteger(0);

    public int nextAudioStreamIndex() {
        return audioStreamIndex.getAndIncrement();
    }

    public int nextVideoStreamIndex() {
        return videoStreamIndex.getAndIncrement();
    }

    public int nextTextStreamIndex() {
        return textStreamIndex.getAndIncrement();
    }
}

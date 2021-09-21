package ru.gadjini.telegram.converter.service.conversion.progress;

import ru.gadjini.telegram.smart.bot.commons.service.process.FFmpegProgressCallback;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FFmpegProgressCallbackHandler implements FFmpegProgressCallback {

    private final AtomicLong start = new AtomicLong(0);

    private Long duration;

    private AtomicInteger lastPercentage = new AtomicInteger(-1);

    public FFmpegProgressCallbackHandler(Long duration) {
        this.duration = duration;
    }

    @Override
    public void progress(int percentage) {
        if (isStartPoint() | isTheSameAsPrevPercentage(percentage)) {
            return;
        }
        if (isTimeToUpdate()) {
            //TODO: Implement update progress
            System.out.println("Progress: " + percentage + "%");
        }
    }

    @Override
    public Long duration() {
        return duration;
    }

    private boolean isTheSameAsPrevPercentage(int percentage) {
        int lastPercent = lastPercentage.get();
        lastPercentage.set(percentage);

        return lastPercent == percentage;
    }

    private boolean isTimeToUpdate() {
        long epochSecond = Instant.now().getEpochSecond();
        long diff = epochSecond - start.get();
        if (diff >= 10) {
            System.out.println(diff);
            start.set(epochSecond);

            return true;
        }

        return false;
    }

    private boolean isStartPoint() {
        if (start.get() == 0) {
            start.set(Instant.now().getEpochSecond());

            return true;
        }

        return false;
    }
}

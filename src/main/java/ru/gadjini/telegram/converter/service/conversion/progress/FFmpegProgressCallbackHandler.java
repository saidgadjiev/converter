package ru.gadjini.telegram.converter.service.conversion.progress;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import ru.gadjini.telegram.smart.bot.commons.service.process.FFmpegProgressCallback;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class FFmpegProgressCallbackHandler implements FFmpegProgressCallback {

    public static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .printZeroIfSupported()
            .minimumPrintedDigits(2)
            .rejectSignedValues(true)
            .appendHours()
            .appendSeparator(":")
            .appendMinutes()
            .appendSeparator(":")
            .appendSeconds()
            .toFormatter();

    private final AtomicLong start = new AtomicLong(0);

    private Long duration;

    private long timeToUpdate;

    private AtomicInteger lastPercentage = new AtomicInteger(-1);

    FFmpegProgressCallbackHandler(Long duration, long timeToUpdate) {
        this.duration = duration;
        this.timeToUpdate = timeToUpdate;
    }

    @Override
    public void progress(int timeLeft, int percentage, Double speed) {
        if (isStartPoint() | isBadPercentage(percentage)) {
            return;
        }
        if (isTimeToUpdate()) {
            int eta = (int) (timeLeft / speed);
            String etaString = eta > 0 ? PERIOD_FORMATTER.print(Period.seconds((int) (timeLeft / speed)).normalizedStandard()) : "N/A";
            String speedString = speed == null ? "N/A" : speed + "x";

            updateProgressMessage(etaString, speedString, percentage);
        }
    }

    @Override
    public Long duration() {
        return duration;
    }

    private boolean isBadPercentage(int percentage) {
        int lastPercent = lastPercentage.get();
        lastPercentage.set(percentage);

        return lastPercent >= percentage;
    }

    private boolean isTimeToUpdate() {
        long epochSecond = Instant.now().getEpochSecond();
        long diff = epochSecond - start.get();
        if (diff >= timeToUpdate) {
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

    abstract void updateProgressMessage(String eta, String speed, int percentage);
}

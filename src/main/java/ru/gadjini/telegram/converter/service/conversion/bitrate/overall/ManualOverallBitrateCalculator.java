package ru.gadjini.telegram.converter.service.conversion.bitrate.overall;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.bitrate.BitrateCalculatorContext;

import java.io.File;

@Component
@Order(3)
public class ManualOverallBitrateCalculator implements OverallBitrateCalculator {

    @Override
    public Integer calculate(BitrateCalculatorContext bitrateCalculatorContext) {
        long fileSize = new File(bitrateCalculatorContext.getIn()).length();

        if (bitrateCalculatorContext.getWhd().getDuration() == null) {
            return null;
        }

        return calculateBitRate(fileSize, bitrateCalculatorContext.getWhd().getDuration());
    }

    private int calculateBitRate(long fileSize, long duration) {
        return (int) ((fileSize * 8) / duration);
    }
}

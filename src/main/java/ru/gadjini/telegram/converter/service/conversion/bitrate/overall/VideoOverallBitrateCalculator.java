package ru.gadjini.telegram.converter.service.conversion.bitrate.overall;

import ru.gadjini.telegram.converter.service.conversion.bitrate.BitrateCalculatorContext;

public interface VideoOverallBitrateCalculator {

    Integer calculate(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException;
}

package ru.gadjini.telegram.converter.service.conversion.bitrate;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public interface BitrateCalculator {

    void prepareContext(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException;

    Integer calculateBitrate(FFprobeDevice.FFProbeStream stream, BitrateCalculatorContext bitrateCalculatorContext);
}

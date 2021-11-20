package ru.gadjini.telegram.converter.service.conversion.bitrate.overall;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.bitrate.BitrateCalculatorContext;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;

@Component
@Order(2)
public class FFmpegVideoOverallBitrateCalculator implements VideoOverallBitrateCalculator {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegVideoOverallBitrateCalculator(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    @Override
    public Integer calculate(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException {
        return fFmpegDevice.getOverallBitrate(bitrateCalculatorContext.getIn());
    }
}

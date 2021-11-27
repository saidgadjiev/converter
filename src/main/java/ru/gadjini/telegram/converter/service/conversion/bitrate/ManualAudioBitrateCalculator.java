package ru.gadjini.telegram.converter.service.conversion.bitrate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.bitrate.overall.OverallBitrateCalculator;
import ru.gadjini.telegram.converter.service.conversion.bitrate.searcher.BitrateGuesser;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegWdhService;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(2)
public class ManualAudioBitrateCalculator implements BitrateCalculator {

    private List<OverallBitrateCalculator> overallBitrateCalculators;

    private FFmpegWdhService fFmpegWdhService;

    @Autowired
    public ManualAudioBitrateCalculator(List<OverallBitrateCalculator> overallBitrateCalculators,
                                        FFmpegWdhService fFmpegWdhService) {
        this.overallBitrateCalculators = overallBitrateCalculators;
        this.fFmpegWdhService = fFmpegWdhService;
    }

    @Override
    public void prepareContext(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException {
        long durationInSeconds = fFmpegWdhService.getDurationInSeconds(bitrateCalculatorContext.getIn());
        FFprobeDevice.WHD whd = new FFprobeDevice.WHD();
        whd.setDuration(durationInSeconds);
        bitrateCalculatorContext.setWhd(whd);

        Integer overallBitrate = getOverallBitrate(bitrateCalculatorContext);
        bitrateCalculatorContext.setOverallBitrate(overallBitrate);
    }

    @Override
    public Integer calculateBitrate(FFprobeDevice.FFProbeStream stream, BitrateCalculatorContext bitrateCalculatorContext) {
        if (!bitrateCalculatorContext.isAudioManualBitrateCalculated()) {
            calculateManualBitrate(bitrateCalculatorContext);
            bitrateCalculatorContext.setAudioManualBitrateCalculated(true);
        }
        if (stream.getCodecType().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)) {
            return bitrateCalculatorContext.getAudioBitrate();
        }

        return null;
    }

    private void calculateManualBitrate(BitrateCalculatorContext bitrateCalculatorContext) {
        Integer overallBitrate = bitrateCalculatorContext.getOverallBitrate();
        for (FFprobeDevice.FFProbeStream stream : bitrateCalculatorContext.getStreams()) {
            if (stream.getBitRate() != null) {
                overallBitrate -= stream.getBitRate();
            }
        }

        int audioStreamsCount = (int) bitrateCalculatorContext.getStreams().stream().filter(
                f -> f.getCodecType().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)
                        && f.getBitRate() == null
        ).count();

        AtomicInteger audioBitrateResult = new AtomicInteger();

        BitrateGuesser.guessAudioFileBitrate(overallBitrate, audioStreamsCount, audioBitrateResult);

        if (audioStreamsCount > 0) {
            bitrateCalculatorContext.setAudioBitrate(audioBitrateResult.get());
        }
    }

    private Integer getOverallBitrate(BitrateCalculatorContext calculatorContext) throws InterruptedException {
        for (OverallBitrateCalculator overallBitrateCalculator : overallBitrateCalculators) {
            Integer bitrate = overallBitrateCalculator.calculate(calculatorContext);

            if (bitrate != null) {
                return bitrate;
            }
        }

        throw new IllegalArgumentException("Bitrate can't be calculated for " + calculatorContext.getIn());
    }
}

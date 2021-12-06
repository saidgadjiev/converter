package ru.gadjini.telegram.converter.service.conversion.bitrate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.bitrate.overall.OverallBitrateCalculator;
import ru.gadjini.telegram.converter.service.conversion.bitrate.searcher.AudioBitrateByResolutionSearcher;
import ru.gadjini.telegram.converter.service.conversion.bitrate.searcher.BitrateGuesser;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegImageStreamDetector;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamDetector;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(2)
public class ManualVideoBitrateCalculator implements BitrateCalculator {

    private FFmpegImageStreamDetector imageStreamDetector;

    private List<OverallBitrateCalculator> overallBitrateCalculators;

    private FFmpegVideoStreamDetector videoStreamDetector;

    @Autowired
    public ManualVideoBitrateCalculator(FFmpegImageStreamDetector imageStreamDetector,
                                        List<OverallBitrateCalculator> overallBitrateCalculators, FFmpegVideoStreamDetector videoStreamDetector) {
        this.imageStreamDetector = imageStreamDetector;
        this.overallBitrateCalculators = overallBitrateCalculators;
        this.videoStreamDetector = videoStreamDetector;
    }

    @Override
    public void prepareContext(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException {
        if (bitrateCalculatorContext.getTargetFormatCategory() == FormatCategory.VIDEO) {
            FFprobeDevice.WHD whd = videoStreamDetector.getFirstVideoStream(bitrateCalculatorContext.getStreams()).getWhd();
            bitrateCalculatorContext.setWhd(whd);

            Integer overallBitrate = getOverallBitrate(bitrateCalculatorContext);
            bitrateCalculatorContext.setOverallBitrate(overallBitrate);
        }
    }

    @Override
    public Integer calculateBitrate(FFprobeDevice.FFProbeStream stream, BitrateCalculatorContext bitrateCalculatorContext) {
        if (bitrateCalculatorContext.getTargetFormatCategory() != FormatCategory.VIDEO) {
            return null;
        }
        if (!bitrateCalculatorContext.isVideoManualBitrateCalculated()) {
            calculateManualBitrate(bitrateCalculatorContext);
            bitrateCalculatorContext.setVideoManualBitrateCalculated(true);
        }
        if (stream.getCodecType().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)
                && imageStreamDetector.isImageStream(stream)) {
            return bitrateCalculatorContext.getImageVideoBitrate();
        }
        if (stream.getCodecType().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)
                && !imageStreamDetector.isImageStream(stream)) {
            return bitrateCalculatorContext.getVideoBitrate();
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

        int videoStreamsCount = (int) bitrateCalculatorContext.getStreams().stream().filter(
                f -> f.getCodecType().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)
                        && f.getBitRate() == null
                        && !imageStreamDetector.isImageStream(f)
        ).count();
        int imageVideoStreamsCount = (int) bitrateCalculatorContext.getStreams().stream().filter(
                f -> f.getCodecType().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)
                        && f.getBitRate() == null
                        && imageStreamDetector.isImageStream(f)
        ).count();
        int audioStreamsCount = (int) bitrateCalculatorContext.getStreams().stream().filter(
                f -> f.getCodecType().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)
                        && f.getBitRate() == null
        ).count();

        AtomicInteger videoBitrateResult = new AtomicInteger();
        AtomicInteger audioBitrateResult = new AtomicInteger();
        AtomicInteger imageVideoBitrateResult = new AtomicInteger();

        int startAudioBitrate = AudioBitrateByResolutionSearcher.getAudioBitrate(bitrateCalculatorContext.getWhd().getHeight());
        BitrateGuesser.guessVideoFileBitrate(overallBitrate, startAudioBitrate, videoStreamsCount, audioStreamsCount, imageVideoStreamsCount,
                videoBitrateResult, audioBitrateResult, imageVideoBitrateResult);

        if (videoStreamsCount > 0) {
            bitrateCalculatorContext.setVideoBitrate(videoBitrateResult.get());
        }
        if (audioStreamsCount > 0) {
            bitrateCalculatorContext.setAudioBitrate(audioBitrateResult.get());
        }
        if (imageVideoStreamsCount > 0) {
            bitrateCalculatorContext.setImageVideoBitrate(imageVideoBitrateResult.get());
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

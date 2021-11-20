package ru.gadjini.telegram.converter.service.conversion.bitrate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.bitrate.overall.VideoOverallBitrateCalculator;
import ru.gadjini.telegram.converter.service.conversion.bitrate.searcher.AudioBitrateByResolutionSearcher;
import ru.gadjini.telegram.converter.service.conversion.bitrate.searcher.BitrateGuesser;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegImageStreamDetector;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamDetector;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegWdhService;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(2)
public class ManualBitrateCalculator implements BitrateCalculator {

    private FFmpegImageStreamDetector imageStreamDetector;

    private Set<VideoOverallBitrateCalculator> overallBitrateCalculators;

    private FFmpegWdhService fFmpegWdhService;

    private FFmpegVideoStreamDetector videoStreamDetector;

    @Autowired
    public ManualBitrateCalculator(FFmpegImageStreamDetector imageStreamDetector,
                                   Set<VideoOverallBitrateCalculator> overallBitrateCalculators,
                                   FFmpegWdhService fFmpegWdhService, FFmpegVideoStreamDetector videoStreamDetector) {
        this.imageStreamDetector = imageStreamDetector;
        this.overallBitrateCalculators = overallBitrateCalculators;
        this.fFmpegWdhService = fFmpegWdhService;
        this.videoStreamDetector = videoStreamDetector;
    }

    @Override
    public void prepareContext(BitrateCalculatorContext bitrateCalculatorContext) throws InterruptedException {
        FFprobeDevice.WHD whd = fFmpegWdhService.getWHD(bitrateCalculatorContext.getIn(),
                videoStreamDetector.getFirstVideoStreamIndex(bitrateCalculatorContext.getStreams()));
        bitrateCalculatorContext.setWhd(whd);
        Integer overallBitrate = getOverallBitrate(bitrateCalculatorContext);
        for (FFprobeDevice.FFProbeStream stream : bitrateCalculatorContext.getStreams()) {
            if (stream.getBitRate() != null) {
                overallBitrate -= stream.getBitRate();
            }
        }

        int startAudioBitrate = AudioBitrateByResolutionSearcher.getAudioBitrate(bitrateCalculatorContext.getWhd().getHeight());
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
        BitrateGuesser.guessBitrate(overallBitrate, startAudioBitrate, videoStreamsCount, audioStreamsCount, imageVideoStreamsCount,
                videoBitrateResult, audioBitrateResult, imageVideoBitrateResult);

        bitrateCalculatorContext.setVideoBitrate(videoBitrateResult.get());
        bitrateCalculatorContext.setAudioBitrate(audioBitrateResult.get());
        bitrateCalculatorContext.setImageVideoBitrate(imageVideoBitrateResult.get());
    }

    @Override
    public Integer calculateBitrate(FFprobeDevice.FFProbeStream stream, BitrateCalculatorContext bitrateCalculatorContext) {
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

    private Integer getOverallBitrate(BitrateCalculatorContext calculatorContext) throws InterruptedException {
        for (VideoOverallBitrateCalculator overallBitrateCalculator : overallBitrateCalculators) {
            Integer bitrate = overallBitrateCalculator.calculate(calculatorContext);

            if (bitrate != null) {
                return bitrate;
            }
        }

        throw new IllegalArgumentException("Bitrate can't be calculated for " + calculatorContext.getIn());
    }
}

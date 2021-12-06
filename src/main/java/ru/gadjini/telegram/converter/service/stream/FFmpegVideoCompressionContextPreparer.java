package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamDetector;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class FFmpegVideoCompressionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    private static final String SCALE = "scale=-2:ceil(ih/3)*2";

    private FFmpegConversionContextPreparerChain audioInVideoCompression;

    private FFmpegVideoStreamDetector videoStreamDetector;

    public FFmpegVideoCompressionContextPreparer(FFmpegConversionContextPreparerChain audioInVideoCompression,
                                                 FFmpegVideoStreamDetector videoStreamDetector) {
        this.audioInVideoCompression = audioInVideoCompression;
        this.videoStreamDetector = videoStreamDetector;
    }

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            stream.setTargetScale(SCALE);
        }
        conversionContext.setUseCrf(true);

        FFprobeDevice.WHD whd = videoStreamDetector.getFirstVideoStream(conversionContext.streams()).getWhd();
        int targetHeight = (int) (whd.getHeight() / 1.5);
        conversionContext.putExtra(FFmpegConversionContext.TARGET_RESOLUTION, targetHeight);
        audioInVideoCompression.prepare(conversionContext);

        super.prepare(conversionContext);
    }
}

package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.conversion.bitrate.AudioCompressionHelper;
import ru.gadjini.telegram.converter.service.conversion.codec.AudioCodecHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import java.util.List;
import java.util.stream.Collectors;

public class FFmpegAudioInVideoCompressionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        List<Integer> currentAudioBitrate = conversionContext.audioStreams().stream()
                .map(FFprobeDevice.FFProbeStream::getBitRate).collect(Collectors.toList());
        if (currentAudioBitrate.isEmpty()) {
            return;
        }
        int targetResolution = conversionContext.getExtra(FFmpegConversionContext.TARGET_RESOLUTION);
        int audioBitrateForCompression = AudioCompressionHelper.getAudioBitrateForCompression(targetResolution, currentAudioBitrate);

        for (FFprobeDevice.FFProbeStream stream : conversionContext.audioStreams()) {
            stream.setTargetBitrate(audioBitrateForCompression);
            stream.setTargetCodec(stream.getCodecName(), AudioCodecHelper.getCodec(stream.getCodecName()));
        }

        super.prepare(conversionContext);
    }
}

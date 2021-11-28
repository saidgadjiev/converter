package ru.gadjini.telegram.converter.service.stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;

@Component
public class FFmpegConversionContextPreparerChainFactory {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegConversionContextPreparerChainFactory(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public FFmpegConversionContextPreparerChain videoConversionContextPreparer() {
        return new BaseFFmpegConversionContextPreparerChain() {
            @Override
            public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
                telegramVideoContextPreparer().prepare(conversionContext);
                _3gpScaleContextPreparer().prepare(conversionContext);
                subtitlesContextPreparer().prepare(conversionContext);

                super.prepare(conversionContext);
            }
        };
    }

    public FFmpegConversionContextPreparerChain telegramVideoContextPreparer() {
        return new TelegramVideoConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain videoEditorContextPreparer() {
        return new VideoEditorConversionContextProcessor();
    }

    public FFmpegConversionContextPreparerChain _3gpScaleContextPreparer() {
        return new _3gpScaleConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain telegramVoiceContextPreparer() {
        return new FFmpegTelegramVoiceContextPreparer();
    }

    public FFmpegConversionContextPreparerChain bassBoostPreparer() {
        return new FFmpegBassBoostConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain videoWatermark() {
        return new FFmpegVideoWatermarkContextPreparer();
    }

    public FFmpegConversionContextPreparerChain extractAudioPreparer() {
        return new FFmpegExtractAudioContextPreparer(fFmpegDevice);
    }

    public FFmpegConversionContextPreparerChain subtitlesContextPreparer() {
        return new SubtitlesConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain squareVideo() {
        return new FFmpegSquareVideoContextPreparer();
    }

    public FFmpegConversionContextPreparerChain videoCompression() {
        return new FFmpegVideoCompressionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain audioCompression() {
        return new FFmpegAudioCompressionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain vaiMake() {
        return new VaiMakeConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain videoCut() {
        return new VideoCutConversionContextPreparer();
    }
}

package ru.gadjini.telegram.converter.service.stream;

import org.springframework.stereotype.Component;

@Component
public class FFmpegConversionContextPreparerChainFactory {

    public FFmpegConversionContextPreparerChain telegramVideoContextPreparer() {
        return new TelegramVideoConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain videoEditorContextPreparer() {
        return new VideoEditorConversionContextProcessor();
    }

    public FFmpegConversionContextPreparerChain streamScaleContextPreparer() {
        return new StreamScaleConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain telegramVoiceContextPreparer() {
        return new StreamScaleConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain subtitlesContextPreparer() {
        return new SubtitlesConversionContextPreparer();
    }

    public FFmpegConversionContextPreparerChain squareVideo() {
        return new FFmpegSquareVideoContextPreparer();
    }
}

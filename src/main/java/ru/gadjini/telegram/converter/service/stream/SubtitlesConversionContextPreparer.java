package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

public class SubtitlesConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        String subtitlesCodec = null;
        if (conversionContext.outputFormat() == MP4 || conversionContext.outputFormat() == MOV) {
            subtitlesCodec = FFmpegCommand.MOV_TEXT_CODEC;
        } else if (conversionContext.outputFormat() == WEBM) {
            subtitlesCodec = FFmpegCommand.WEBVTT_CODEC;
        } else if (conversionContext.outputFormat() == MKV) {
            subtitlesCodec = FFmpegCommand.SRT_CODEC;
        }
        for (FFprobeDevice.FFProbeStream stream : conversionContext.subtitleStreams()) {
            stream.setTargetCodecName(subtitlesCodec);
        }

        super.prepare(conversionContext);
    }
}

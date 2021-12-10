package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.BitrateUtils;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class FFmpegAudioCompressionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        Integer bitrate = conversionContext.getExtra(FFmpegConversionContext.AUDIO_BITRATE);
        for (FFprobeDevice.FFProbeStream stream : conversionContext.audioStreams()) {
            stream.setTargetBitrate(BitrateUtils.toBytes(bitrate));
            setCodec(stream, conversionContext.outputFormat());
        }

        super.prepare(conversionContext);
    }

    private void setCodec(FFprobeDevice.FFProbeStream stream, Format format) {
        if (format == Format.OPUS) {
            stream.setTargetCodec(FFmpegCommand.OPUS_CODEC_NAME, FFmpegCommand.LIBOPUS);
        } else {
            stream.setTargetCodec(FFmpegCommand.MP3, FFmpegCommand.LIBMP3LAME);
        }
    }
}

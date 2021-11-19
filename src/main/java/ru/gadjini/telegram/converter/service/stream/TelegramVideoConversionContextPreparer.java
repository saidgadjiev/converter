package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class TelegramVideoConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        if (conversionContext.outputFormat().canBeSentAsVideo()) {
            for (FFprobeDevice.FFProbeStream stream : conversionContext.streams()) {
                if (stream.getCodecType().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)) {
                    stream.setTargetCodecName(FFmpegAudioStreamInVideoConversionHelper.TELEGRAM_VIDEO_AUDIO_CODEC);
                } else if (stream.getCodecType().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)) {
                    stream.setTargetCodecName(FFmpegCommand.H264_CODEC);
                    stream.setTargetScale(FFmpegCommand.EVEN_SCALE);
                }
            }
        }

        super.prepare(conversionContext);
    }
}
